package org.apache.solr.extensions.client.lb;

import java.util.List;

import org.apache.solr.extensions.client.SolrServerWrapper;
import org.apache.solr.extensions.client.SolrServerWrapper.SolrServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.google.common.base.Preconditions;

/**
 * Uses a sticky solr server stored in the spring {@linkplain RequestAttributes}
 * . This is particularly useful, if you have subsequent calls to a solr, that
 * might use the same set of filter / document cache.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class StickyDelegatingSolrLoadBalancerStrategy implements SolrLoadBalancingStrategy {

   private final Logger logger = LoggerFactory.getLogger(getClass());

   private final SolrLoadBalancingStrategy delegate;

   public StickyDelegatingSolrLoadBalancerStrategy(SolrLoadBalancingStrategy delegate) {
      Preconditions.checkNotNull(delegate);

      this.delegate = delegate;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public SolrServerWrapper nextAliveSolrServer(List<SolrServerWrapper> servers) {
      Preconditions.checkNotNull(servers);

      // check for attributes. if not present, direct to delegate
      RequestAttributes attributes = null;
      try {
         attributes = RequestContextHolder.currentRequestAttributes();
      } catch (IllegalStateException e) {
         return delegate.nextAliveSolrServer(servers);
      }

      // this is our attribute key
      String attributeKey = getClass().getName() + "-" + servers.hashCode();

      // get stored solr server
      SolrServerWrapper server = (SolrServerWrapper) attributes.getAttribute(attributeKey,
            RequestAttributes.SCOPE_REQUEST);

      // stored server DEAD meanwhile?
      if (server !=  null && SolrServerState.DEAD.equals(server.getState())) {
         server = null;
      }

      // no stored solr server found
      if (server == null) {

         // get unwrapped instance
         server = delegate.nextAliveSolrServer(servers);

         // set for future use
         attributes.setAttribute(attributeKey, server, RequestAttributes.SCOPE_REQUEST);
      } else {
         logger.info(String.format("Reusing %s stored in request context ...", server));
      }

      return server;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void requestEnded(SolrServerWrapper wrapper) {
      delegate.requestEnded(wrapper);
   }

}
