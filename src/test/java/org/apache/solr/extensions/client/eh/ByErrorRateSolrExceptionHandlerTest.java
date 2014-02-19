package org.apache.solr.extensions.client.eh;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.apache.http.client.HttpClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.request.SolrPing;
import org.apache.solr.extensions.client.ExplanatoryHttpSolrServer;
import org.apache.solr.extensions.client.SolrServerWrapper;
import org.apache.solr.extensions.client.SolrServerWrapper.SolrServerState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ByErrorRateSolrExceptionHandlerTest {

   private ByErrorRateSolrExceptionHandler handler;

   private SolrServerWrapper wrapper;

   @Before
   public void setUp() throws Exception {
      handler = new ByErrorRateSolrExceptionHandler();
      handler.setMaximumErrorsPerServerPerMinute(5);

      wrapper = new SolrServerWrapper(new ExplanatoryHttpSolrServer("http://first.url/solr", mock(HttpClient.class)));
   }

   @Test
   public void testImmediateDeath() throws Exception {
      handler.addImmediateDeadException(RuntimeException.class);
      
      SolrRequest request = new SolrPing();
      
      handler.exceptionRaised(request, wrapper, new IllegalArgumentException());
      assertEquals(SolrServerState.ALIVE, wrapper.getState());
      
      handler.exceptionRaised(request, wrapper, new RuntimeException());
      assertEquals(SolrServerState.DEAD, wrapper.getState());
   }
   
   @Test
   public void testDeathByErrorRate() throws Exception {
      SolrRequest request = new SolrPing();
      
      for (int i = 1; i <= 5; i++) {
         handler.exceptionRaised(request, wrapper, new IllegalArgumentException());
         assertEquals(i, wrapper.getExceptions().size());
         assertEquals(SolrServerState.ALIVE, wrapper.getState());
      }
      
      handler.exceptionRaised(request, wrapper, new IllegalArgumentException());
      assertEquals(6, wrapper.getExceptions().size());
      assertEquals(SolrServerState.DEAD, wrapper.getState());
   }
}
