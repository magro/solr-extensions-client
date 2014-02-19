package org.apache.solr.extensions.client.ck;

import java.util.Collection;

import org.apache.solr.extensions.client.SolrServerWrapper;

/**
 * Iterates through a list of solr servers and checks whether they are alive or
 * not.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public interface SolrServerAliveChecker {

   int getCheckIntervalInSeconds();
   
   void check(Collection<SolrServerWrapper> servers);

}
