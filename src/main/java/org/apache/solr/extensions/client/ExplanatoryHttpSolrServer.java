package org.apache.solr.extensions.client;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.ResponseParser;
import org.apache.solr.client.solrj.impl.HttpSolrServer;

import com.google.common.base.Objects;

/**
 * A solr server that does a proper {@linkplain #toString()}
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class ExplanatoryHttpSolrServer extends HttpSolrServer {

   private static final long serialVersionUID = -4775137198873825497L;

   /**
    * {@inheritDoc}
    */
   public ExplanatoryHttpSolrServer(String baseURL, HttpClient client) {
      super(baseURL, client);
   }

   /**
    * {@inheritDoc}
    */
   public ExplanatoryHttpSolrServer(String baseURL, HttpClient client, ResponseParser parser) {
      super(baseURL, client, parser);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public String toString() {
      return Objects.toStringHelper(this).add("url", getBaseURL()).toString();
   }

   
}
