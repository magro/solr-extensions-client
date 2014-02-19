package org.apache.solr.extensions.client.lb;

import java.util.List;

import org.apache.solr.extensions.client.SolrServerWrapper;


/**
 * Strategy for choosing the next sorl server to issue a request on.
 * 
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public interface SolrLoadBalancingStrategy {

   SolrServerWrapper nextAliveSolrServer(List<SolrServerWrapper> servers);

   void requestEnded(SolrServerWrapper wrapper);

}
