package org.apache.solr.extensions.client.ck;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SolrServerAliveCheckerSupportTest {

   @Test
   public void testStoresCheckInterval() throws Exception {
      SolrServerAliveCheckerSupport checker = new SolrServerAliveCheckerSupport(42);
      assertEquals(42, checker.getCheckIntervalInSeconds());
   }

}
