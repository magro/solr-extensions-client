package org.apache.solr.extensions.client.ck;

import java.util.Collection;

import org.apache.solr.extensions.client.SolrServerWrapper;
import org.apache.solr.extensions.client.SolrServerWrapper.SolrServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Iterates through all servers and checks whether they respond to ping
 * requests. Irresponisble servers are marked dead immediately.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class PingAwareSolrServerAliveChecker extends SolrServerAliveCheckerSupport {

   private final Logger logger = LoggerFactory.getLogger(getClass());

   public PingAwareSolrServerAliveChecker(int checkIntervalInSeconds) {
      super(checkIntervalInSeconds);
   }

   @Override
   public void check(Collection<SolrServerWrapper> servers) {
      Preconditions.checkNotNull(servers);

      for (SolrServerWrapper wrapper : servers) {

         // ping server
         try {
            wrapper.getServer().ping();

            if (SolrServerState.DEAD.equals(wrapper.getState())) {
               logger.info("Marking Solr {} alive ...", wrapper.getServer());
               wrapper.setState(SolrServerState.ALIVE);
            }
         } catch (Exception e) {
            logger.info("Marking Solr {} dead ({}): {}", wrapper.getServer(), e.getClass().getSimpleName(),
                  e.getMessage());
            wrapper.setState(SolrServerState.DEAD);
         }
      }

   }

}
