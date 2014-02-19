package org.apache.solr.extensions.client.eh;

import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.extensions.client.SolrServerWrapper;

public interface SolrExceptionHandler {

   void exceptionRaised(SolrRequest request, SolrServerWrapper wrapper, Exception e);

}
