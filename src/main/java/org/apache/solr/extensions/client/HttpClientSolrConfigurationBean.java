package org.apache.solr.extensions.client;

import java.util.concurrent.TimeUnit;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Preconditions;

/**
 * Uses the Spring framework to configure a given {@linkplain HttpClient} with
 * some defaults that should fit a Solr environment.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
@Component
public class HttpClientSolrConfigurationBean implements InitializingBean {

   private final Logger logger = LoggerFactory.getLogger(getClass());

   // defaults
   private static final boolean DEFAULT_HTTP_CONNECTION_KEEPALIVE = true;
   private static final int DEFAULT_HTTP_CONNECTION_KEEPALIVE_MS = (int) TimeUnit.SECONDS.toMillis(120l);
   private static final boolean DEFAULT_HTTP_FOLLOW_REDIRECTS = false;

   // http connection timeout (Determines the timeout until a connection is
   // established.)
   private static final int DEFAULT_HTTP_CONNECTION_TIMEOUT_MS = (int) TimeUnit.MILLISECONDS.toMillis(100l);
   private static final int DEFAULT_HTTP_BANNER_TIMEOUT_MS = (int) TimeUnit.MILLISECONDS.toMillis(10l);

   // solr http socket timeout (timeout for waiting for data)
   private static final int DEFAULT_HTTP_SOCKET_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(45l);

   // configurables
   private int connectionTimeoutMilliSeconds = DEFAULT_HTTP_CONNECTION_TIMEOUT_MS;
   private int socketTimeoutMilliSeconds = DEFAULT_HTTP_SOCKET_TIMEOUT_MS;
   private int bannerTimeoutMilliSeconds = DEFAULT_HTTP_BANNER_TIMEOUT_MS;
   private boolean followRedirects = DEFAULT_HTTP_FOLLOW_REDIRECTS;
   private boolean keepAlive = DEFAULT_HTTP_CONNECTION_KEEPALIVE;
   private int keepAliveMilliSeconds = DEFAULT_HTTP_CONNECTION_KEEPALIVE_MS;
   private boolean useRetry = false;

   @Autowired
   private DefaultHttpClient httpClient;

   @Override
   public void afterPropertiesSet() throws Exception {
      Preconditions.checkNotNull(httpClient);

      // redirects
      logger.info("Setting followRedirects to {} ...", followRedirects);
      httpClient.getParams().setBooleanParameter("followRedirects", followRedirects);

      // set timeouts
      logger.info("Setting socket timeout (wait for data) to {}ms ...", socketTimeoutMilliSeconds);
      HttpClientUtil.setSoTimeout(httpClient, (int) socketTimeoutMilliSeconds);
      logger.info("Setting connection timeout (wait for connection) to {}ms ...", connectionTimeoutMilliSeconds);
      HttpClientUtil.setConnectionTimeout(httpClient, (int) connectionTimeoutMilliSeconds);
      logger.info("Setting banner connection timeout (wait for banner bytes) to {}ms ...", bannerTimeoutMilliSeconds);
      httpClient.getParams().setIntParameter("http.banner.timeout", bannerTimeoutMilliSeconds);

      logger.info("Setting useRetry to {} ...", useRetry);
      HttpClientUtil.setUseRetry(httpClient, useRetry);

      // force keep alive
      if (keepAlive) {
         httpClient.setReuseStrategy(new ConnectionReuseStrategy() {
            @Override
            public boolean keepAlive(HttpResponse response, HttpContext context) {
               return keepAlive;
            }
         });
         httpClient.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
            @Override
            public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
               return keepAliveMilliSeconds;
            }
         });
      }

   }

   // setters

   public void setConnectionTimeoutMilliSeconds(int connectionTimeoutMilliSeconds) {
      this.connectionTimeoutMilliSeconds = connectionTimeoutMilliSeconds;
   }

   public void setSocketTimeoutMilliSeconds(int socketTimeoutMilliSeconds) {
      this.socketTimeoutMilliSeconds = socketTimeoutMilliSeconds;
   }

   public void setFollowRedirects(boolean followRedirects) {
      this.followRedirects = followRedirects;
   }

   public void setKeepAlive(boolean keepAlive) {
      this.keepAlive = keepAlive;
   }

   public void setKeepAliveMilliSeconds(int keepAliveMilliSeconds) {
      this.keepAliveMilliSeconds = keepAliveMilliSeconds;
   }

   public void setHttpClient(DefaultHttpClient httpClient) {
      this.httpClient = httpClient;
   }

   public void setUseRetry(boolean useRetry) {
      this.useRetry = useRetry;
   }

   public void setBannerTimeoutMilliSeconds(int bannerTimeoutMilliSeconds) {
      this.bannerTimeoutMilliSeconds = bannerTimeoutMilliSeconds;
   }

}
