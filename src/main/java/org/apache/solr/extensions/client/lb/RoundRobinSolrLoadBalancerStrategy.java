package org.apache.solr.extensions.client.lb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.solr.extensions.client.SolrServerWrapper;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * Simple round robin implementation.
 * 
 * @author Shopping24 GmbH, Torsten Bøgh Köster (@tboeghk)
 */
public class RoundRobinSolrLoadBalancerStrategy implements SolrLoadBalancingStrategy {

   private final AtomicInteger counter = new AtomicInteger(0);

   /**
    * Selects the next alive solr server by round robininin through them.
    */
   @Override
   public SolrServerWrapper nextAliveSolrServer(List<SolrServerWrapper> servers) {

      // filter for live solr servers
      ArrayList<SolrServerWrapper> candidates = Lists
            .newArrayList(Iterables.filter(servers, SolrServerWrapper.alive()));
      Preconditions.checkArgument(!candidates.isEmpty());

      return candidates.get(counter.getAndIncrement() % candidates.size());
   }

   @Override
   public void requestEnded(SolrServerWrapper wrapper) {
      // noop
   }

}
