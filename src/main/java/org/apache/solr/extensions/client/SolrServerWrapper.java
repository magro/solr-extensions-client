package org.apache.solr.extensions.client;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.solr.client.solrj.SolrServer;

import com.google.common.base.Objects;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Wraps a HttpSolrServer and stores meta information.
 */
public class SolrServerWrapper {

   public enum SolrServerState {

      // server is alive. Use it
      ALIVE,

      // the server is down. Don't use it.
      DEAD,
   }

   private final Cache<Date, Exception> exceptions = CacheBuilder.newBuilder().expireAfterWrite(5l, TimeUnit.MINUTES)
         .weakKeys().weakValues().build();

   private final SolrServer server;
   private SolrServerState state = SolrServerState.ALIVE;

   public SolrServerWrapper(SolrServer solrServer) {
      this.server = solrServer;
   }

   public SolrServerState getState() {
      return state;
   }

   public void setState(SolrServerState state) {
      this.state = state;
   }

   public SolrServer getServer() {
      return server;
   }

   public Map<Date, Exception> getExceptions() {
      return exceptions.asMap();
   }

   // --- Predicates --------------------------------------------------
   public static Predicate<SolrServerWrapper> alive() {
      return new Predicate<SolrServerWrapper>() {
         @Override
         public boolean apply(SolrServerWrapper wrapper) {
            return wrapper != null && SolrServerState.ALIVE.equals(wrapper.getState());
         }
      };
   }

   @Override
   public String toString() {
      return Objects.toStringHelper(this).add("server", getServer()).add("state", getState()).toString();
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((server == null) ? 0 : server.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof SolrServerWrapper)) {
         return false;
      }
      SolrServerWrapper other = (SolrServerWrapper) obj;
      if (server == null) {
         if (other.server != null) {
            return false;
         }
      } else if (!server.equals(other.server)) {
         return false;
      }
      return true;
   }
}