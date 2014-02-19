package org.apache.solr.extensions.client.ck;

import java.util.Collection;

import org.apache.solr.extensions.client.SolrServerWrapper;
import org.apache.solr.extensions.client.SolrServerWrapper.SolrServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * Iterates through the dead servers and checks whether they are back in
 * business, ignores alive servers.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class DeadSolrServerAliveChecker extends SolrServerAliveCheckerSupport {

   private final Logger logger = LoggerFactory.getLogger(getClass());

   public DeadSolrServerAliveChecker(int checkIntervalInSeconds) {
      super(checkIntervalInSeconds);
   }

   @Override
   public void check(Collection<SolrServerWrapper> servers) {
      Preconditions.checkNotNull(servers);

      for (SolrServerWrapper wrapper : Collections2.filter(servers, zombie())) {

         // ping server
         try {
            wrapper.getServer().ping();

            if (SolrServerState.DEAD.equals(wrapper.getState())) {
               logger.info("Marking Solr {} alive ...", wrapper.getServer());
               wrapper.setState(SolrServerState.ALIVE);
            }
         } catch (Exception e) {
            logger.info("Solr {} still unreachable ({}): {}", wrapper.getServer(), e.getClass().getSimpleName(),
                  e.getMessage());
         }
      }
   }

   public static Predicate<SolrServerWrapper> zombie() {
      return new Predicate<SolrServerWrapper>() {
         @Override
         public boolean apply(SolrServerWrapper wrapper) {
            return SolrServerState.DEAD.equals(wrapper.getState());
         }
      };
   }

}
