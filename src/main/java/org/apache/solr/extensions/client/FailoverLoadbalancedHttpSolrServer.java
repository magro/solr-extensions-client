package org.apache.solr.extensions.client;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.BinaryResponseParser;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SolrjNamedThreadFactory;
import org.apache.solr.extensions.client.ck.PingAwareSolrServerAliveChecker;
import org.apache.solr.extensions.client.ck.SolrServerAliveChecker;
import org.apache.solr.extensions.client.eh.ByErrorRateSolrExceptionHandler;
import org.apache.solr.extensions.client.eh.SolrExceptionHandler;
import org.apache.solr.extensions.client.lb.RoundRobinSolrLoadBalancerStrategy;
import org.apache.solr.extensions.client.lb.SolrLoadBalancingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * Does a loadbalancing by delegating server election to a
 * {@linkplain SolrLoadBalancingStrategy}, error handling to a
 * {@linkplain SolrExceptionHandler} and alive checking to a
 * {@linkplain SolrServerAliveChecker}.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class FailoverLoadbalancedHttpSolrServer extends SolrServer {

   private static final long serialVersionUID = 7404546748681517879L;

   private final Logger logger = LoggerFactory.getLogger(getClass());

   public enum SolrServerRequestContinuation {
      NOOP,
      RETRY,
      ABORT
   }

   // we use this http client across all configured servers
   protected final HttpClient httpClient;
   protected final ResponseParser responseParser;

   // this is list of configured servers
   private final List<SolrServerWrapper> servers = Lists.newArrayList();

   private final ScheduledExecutorService aliveCheckExecutor;
   private final SolrExceptionHandler exceptionHandler;
   private final SolrLoadBalancingStrategy loadBalancingStrategy;
   private final SolrServerAliveChecker serverWrapperAliveChecker;

   /**
    * Creates a new loadbalanced http solr server with a
    * {@linkplain RoundRobinSolrLoadBalancerStrategy} and a
    * {@linkplain ByErrorRateSolrExceptionHandler}.
    */
   public FailoverLoadbalancedHttpSolrServer(HttpClient httpClient) {
      this(httpClient, new BinaryResponseParser(), new RoundRobinSolrLoadBalancerStrategy(),
            new ByErrorRateSolrExceptionHandler(),
            new PingAwareSolrServerAliveChecker(5));
   }

   public FailoverLoadbalancedHttpSolrServer(HttpClient httpClient, ResponseParser parser,
         SolrLoadBalancingStrategy loadBalancingStrategy,
         SolrExceptionHandler exceptionHandler, SolrServerAliveChecker serverAliveChecker) {
      super();

      // preconditions
      Preconditions.checkNotNull(loadBalancingStrategy);
      Preconditions.checkNotNull(exceptionHandler);
      Preconditions.checkNotNull(serverAliveChecker);
      Preconditions.checkNotNull(httpClient);
      Preconditions.checkNotNull(parser);

      this.loadBalancingStrategy = loadBalancingStrategy;
      this.exceptionHandler = exceptionHandler;
      this.serverWrapperAliveChecker = serverAliveChecker;
      this.httpClient = httpClient;
      this.responseParser = parser;

      this.aliveCheckExecutor = Executors
            .newSingleThreadScheduledExecutor(new SolrjNamedThreadFactory("aliveCheckExecutor"));

      int interval = getAliveChecker().getCheckIntervalInSeconds();
      logger.info("Scheduling SolrServerAliveChecker {} every {} seconds", serverAliveChecker.getClass()
            .getCanonicalName(), interval);
      this.aliveCheckExecutor.scheduleAtFixedRate(new Runnable() {
         @Override
         public void run() {
            // check servers
            getAliveChecker().check(getServers());
         }
      }, interval,
            interval, TimeUnit.SECONDS);
   }

   /**
    * Selects a solr server suitable for the given requests and executes the
    * request on it.
    */
   @Override
   public NamedList<Object> request(SolrRequest request) throws SolrServerException, IOException {
      Preconditions.checkNotNull(request);

      // find next alive solr server
      SolrServerWrapper wrapper = nextAliveSolrServer(request);

      // on exception, report it.
      SolrServerRequestContinuation continuation = SolrServerRequestContinuation.NOOP;
      Exception exception = null;
      NamedList<Object> response = null;
      try {
         response = wrapper.getServer().request(request);
         continuation = requestEnded(request, wrapper, null);
      } catch (Exception e) {
         continuation = requestEnded(request, wrapper, e);
         exception = e;
      }

      // Recover by calling the method again
      if (SolrServerRequestContinuation.ABORT.equals(continuation)) {
         logger.info("Aborting request {}", request);

         if (exception != null) {
            throw new SolrServerException(exception);
         }
      } else if (SolrServerRequestContinuation.RETRY.equals(continuation)) {
         logger.info("Aborting request {}", request);
         return request(request);
      }

      return response;
   }

   /**
    * Adds a solr endpoint url
    */
   public void addSolrServer(String url) {
      Preconditions.checkNotNull(url);

      servers.add(new SolrServerWrapper(createNewSolrServer(httpClient, url)));
   }

   /**
    * Sets a bunch of solr servers. Use in ioc containers.
    */
   public void setSolrServers(Collection<String> urls) {
      Preconditions.checkNotNull(urls);

      for (String url : urls) {
         addSolrServer(url);
      }
   }

   /**
    * Selects the next solr server to direct a request to.
    * 
    * @param request
    *           the request to distribute.
    * @return the {@linkplain SolrServerWrapper} from the list of configured
    *         solrs.
    */
   protected SolrServerWrapper nextAliveSolrServer(SolrRequest request) {
      return loadBalancingStrategy.nextAliveSolrServer(getServers());
   }

   /**
    * Notification method that is called once a request has been handled by a
    * {@linkplain SolrServer}. If the server threw an Exception, it is supplied.
    * 
    * @param request
    *           the request handled
    * @param wrapper
    *           the server that executed the request
    * @param e
    *           the Exception the server threw while handling or
    *           <code>null</code> if request handling was successfull.
    */
   protected SolrServerRequestContinuation requestEnded(SolrRequest request, SolrServerWrapper wrapper, Exception e) {
      Preconditions.checkNotNull(request);
      Preconditions.checkNotNull(wrapper);

      // default's to do nothing
      SolrServerRequestContinuation continuation = SolrServerRequestContinuation.NOOP;

      if (e != null) {
         exceptionHandler.exceptionRaised(request, wrapper, e);
         continuation = SolrServerRequestContinuation.RETRY;
      }

      return continuation;
   }

   /**
    * Creates a new {@linkplain SolrServer} to distribute requests on.
    * 
    * @param httpClient
    *           the http client to use
    * @param url
    *           the endpoint of the given solr server
    * @return the fully created solr server.
    */
   protected HttpSolrServer createNewSolrServer(HttpClient httpClient, String url) {
      return new ExplanatoryHttpSolrServer(url, httpClient, responseParser);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void shutdown() {
      try {
         this.aliveCheckExecutor.shutdownNow();
      } catch (Exception e) {
         logger.warn(e.getMessage(), e);
      }
      try {
         this.httpClient.getConnectionManager().shutdown();
      } catch (Exception e) {
         logger.warn(e.getMessage(), e);
      }
   }

   // --- allow clients and tests to gather insights.

   protected List<SolrServerWrapper> getServers() {
      return servers;
   }

   /**
    * Returns a {@linkplain SolrServerAliveChecker} that checks the configured
    * servers for availability. The runnable is responsive for taking servers
    * back into load balancing and putting them out of it.
    */
   protected SolrServerAliveChecker getAliveChecker() {
      return serverWrapperAliveChecker;
   }

   protected SolrExceptionHandler getExceptionHandler() {
      return exceptionHandler;
   }

   protected SolrLoadBalancingStrategy getLoadBalancingStrategy() {
      return loadBalancingStrategy;
   }

   protected ResponseParser getResponseParser() {
      return responseParser;
   }

}
