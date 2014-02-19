package org.apache.solr.extensions.client;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.impl.BinaryResponseParser;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.extensions.client.FailoverLoadbalancedHttpSolrServer.SolrServerRequestContinuation;
import org.apache.solr.extensions.client.ck.PingAwareSolrServerAliveChecker;
import org.apache.solr.extensions.client.ck.SolrServerAliveChecker;
import org.apache.solr.extensions.client.ck.SolrServerAliveCheckerSupport;
import org.apache.solr.extensions.client.eh.ByErrorRateSolrExceptionHandler;
import org.apache.solr.extensions.client.eh.SolrExceptionHandler;
import org.apache.solr.extensions.client.lb.RoundRobinSolrLoadBalancerStrategy;
import org.apache.solr.extensions.client.lb.SolrLoadBalancingStrategy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FailoverLoadbalancedHttpSolrServerTest {

   @Mock
   private HttpClient httpClient;

   @Mock
   private HttpSolrServer httpSolrServer;
   @Mock
   private ScheduledExecutorService aliveCheckExecutor;
   @Mock
   private SolrExceptionHandler exceptionHandler;
   @Mock
   private SolrLoadBalancingStrategy loadBalancingStrategy;

   private SolrServerAliveChecker serverWrapperAliveChecker = new SolrServerAliveCheckerSupport(42);

   private FailoverLoadbalancedHttpSolrServer server;

   @Before
   public void setUp() throws Exception {
      server = new FailoverLoadbalancedHttpSolrServer(httpClient, new BinaryResponseParser(), loadBalancingStrategy,
            exceptionHandler, serverWrapperAliveChecker);
   }

   @After
   public void tearDown() {
      server.shutdown();
   }

   @Test
   public void testDefaultConstructor() throws Exception {
      server = new FailoverLoadbalancedHttpSolrServer(httpClient);
      assertEquals(BinaryResponseParser.class, server.getResponseParser().getClass());
      assertEquals(RoundRobinSolrLoadBalancerStrategy.class, server.getLoadBalancingStrategy().getClass());
      assertEquals(ByErrorRateSolrExceptionHandler.class, server.getExceptionHandler().getClass());
      assertEquals(PingAwareSolrServerAliveChecker.class, server.getAliveChecker().getClass());
   }

   @Test
   public void testCreateHttpSolrServer() throws Exception {
      HttpSolrServer solrServer = server.createNewSolrServer(httpClient, "http://some.solr.url/solr");
      assertEquals(ExplanatoryHttpSolrServer.class, solrServer.getClass());
      assertEquals("http://some.solr.url/solr", solrServer.getBaseURL());
      assertEquals(httpClient, solrServer.getHttpClient());
   }

   @Test
   public void testRequestEndedWithoutException() throws Exception {
      assertEquals(SolrServerRequestContinuation.NOOP,
            server.requestEnded(new SolrPing(), new SolrServerWrapper(httpSolrServer), null));
   }

   @Test
   public void testRequestEndedWithExceptionAndIsDelegatedToExceptionHandler() throws Exception {
      SolrPing request = new SolrPing();
      SolrServerWrapper wrapper = new SolrServerWrapper(httpSolrServer);
      
      IOException e = new IOException();
      server.requestEnded(request, wrapper, e);

      verify(exceptionHandler).exceptionRaised(eq(request), eq(wrapper), eq(e));
   }

}
