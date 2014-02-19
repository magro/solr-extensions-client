package org.apache.solr.extensions.client;

import static org.junit.Assert.assertEquals;

import org.apache.http.client.HttpClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ExplanatoryHttpSolrServerTest {

   @Mock
   private HttpClient httpClient;

   @Test
   public void testExplanatoryToString() throws Exception {
      ExplanatoryHttpSolrServer server = new ExplanatoryHttpSolrServer("http://some.host/solr", httpClient);
      assertEquals("ExplanatoryHttpSolrServer{url=http://some.host/solr}", server.toString());
   }

}
