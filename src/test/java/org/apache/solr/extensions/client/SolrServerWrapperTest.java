package org.apache.solr.extensions.client;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.apache.http.client.HttpClient;
import org.apache.solr.extensions.client.SolrServerWrapper.SolrServerState;
import org.junit.Before;
import org.junit.Test;

public class SolrServerWrapperTest {

   private SolrServerWrapper wrapper;

   @Before
   public void setUp() throws Exception {
      wrapper = new SolrServerWrapper(new ExplanatoryHttpSolrServer("http://some.url/solr", mock(HttpClient.class)));
   }

   @Test
   public void testDefaults() throws Exception {
      assertEquals(SolrServerState.ALIVE, wrapper.getState());
      assertEquals("ExplanatoryHttpSolrServer{url=http://some.url/solr}", wrapper.getServer().toString());
      assertEquals(0, wrapper.getExceptions().size());
   }
   
   @Test
   public void testPrettyToString() throws Exception {
      assertEquals("SolrServerWrapper{server=ExplanatoryHttpSolrServer{url=http://some.url/solr}, state=ALIVE}", wrapper.toString());
   }

}
