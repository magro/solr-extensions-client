package org.apache.solr.extensions.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.junit.Before;
import org.junit.Test;

public class HttpClientSolrConfigurationBeanTest {

   private HttpClientSolrConfigurationBean bean;
   private DefaultHttpClient httpClient;

   @Before
   public void setUp() throws Exception {
      httpClient = new DefaultHttpClient();
      bean = new HttpClientSolrConfigurationBean();
      bean.setHttpClient(httpClient);
   }

   @Test
   public void testDefaults() throws Exception {
      bean.afterPropertiesSet();

      assertFalse(httpClient.getParams().getBooleanParameter("followRedirects", true));
      assertFalse(httpClient.getHttpRequestRetryHandler().retryRequest(new UnknownHostException(), 0,
            new BasicHttpContext()));

      // timeouts
      assertEquals(45000, httpClient.getParams().getIntParameter(CoreConnectionPNames.SO_TIMEOUT, 0));
      assertEquals(100, httpClient.getParams().getIntParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 0));
      assertEquals(10, httpClient.getParams().getIntParameter("http.banner.timeout", 0));

      // keep alive
      assertTrue(httpClient.getConnectionReuseStrategy().keepAlive(null, null));
      assertEquals(120000, httpClient.getConnectionKeepAliveStrategy().getKeepAliveDuration(null, null));
   }

}
