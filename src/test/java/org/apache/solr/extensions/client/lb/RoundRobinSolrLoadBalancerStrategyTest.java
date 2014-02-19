package org.apache.solr.extensions.client.lb;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.solr.extensions.client.ExplanatoryHttpSolrServer;
import org.apache.solr.extensions.client.SolrServerWrapper;
import org.apache.solr.extensions.client.SolrServerWrapper.SolrServerState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class RoundRobinSolrLoadBalancerStrategyTest {

   private List<SolrServerWrapper> servers;
   
   private RoundRobinSolrLoadBalancerStrategy strategy;

   @Before
   public void setUp() throws Exception {
      servers = Lists.newArrayList();

      servers
            .add(new SolrServerWrapper(new ExplanatoryHttpSolrServer("http://first.host/solr", mock(HttpClient.class))));
      servers
            .add(new SolrServerWrapper(new ExplanatoryHttpSolrServer("http://second.host/solr", mock(HttpClient.class))));
      servers
            .add(new SolrServerWrapper(new ExplanatoryHttpSolrServer("http://third.host/solr", mock(HttpClient.class))));
      
      strategy = new RoundRobinSolrLoadBalancerStrategy();
   }
   
   @Test
   public void testRoundRobinWithAliveServers() throws Exception {
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(1), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(2), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
   }
   
   @Test
   public void testRoundRobinWithDeadServer() throws Exception {
      servers.get(1).setState(SolrServerState.DEAD);
      
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(2), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
   }

   @Test(expected=IllegalArgumentException.class)
   public void testRoundRobinWithAllServersDead() throws Exception {
      servers.get(0).setState(SolrServerState.DEAD);
      servers.get(1).setState(SolrServerState.DEAD);
      servers.get(2).setState(SolrServerState.DEAD);
      
      strategy.nextAliveSolrServer(servers);
   }

}
