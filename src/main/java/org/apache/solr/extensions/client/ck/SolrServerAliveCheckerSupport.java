package org.apache.solr.extensions.client.ck;

import java.util.Collection;

import org.apache.solr.extensions.client.SolrServerWrapper;

/**
 * Convenience super class.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class SolrServerAliveCheckerSupport implements SolrServerAliveChecker {

   private final int checkIntervalInSeconds;
   
   public SolrServerAliveCheckerSupport(int checkIntervalInSeconds) {
      this.checkIntervalInSeconds = checkIntervalInSeconds;
   }

   @Override
   public int getCheckIntervalInSeconds() {
      return checkIntervalInSeconds;
   }

   @Override
   public void check(Collection<SolrServerWrapper> servers) {
      // noop
   }

}
