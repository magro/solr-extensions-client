package org.apache.solr.extensions.client.lb;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.apache.http.client.HttpClient;
import org.apache.solr.extensions.client.ExplanatoryHttpSolrServer;
import org.apache.solr.extensions.client.SolrServerWrapper;
import org.apache.solr.extensions.client.SolrServerWrapper.SolrServerState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.google.common.collect.Lists;

@RunWith(MockitoJUnitRunner.class)
public class StickyDelegatingSolrLoadBalancerStrategyTest {

   private List<SolrServerWrapper> servers;

   private StickyDelegatingSolrLoadBalancerStrategy strategy;

   @Before
   public void setUp() throws Exception {
      servers = Lists.newArrayList();

      servers
            .add(new SolrServerWrapper(new ExplanatoryHttpSolrServer("http://first.host/solr", mock(HttpClient.class))));
      servers
            .add(new SolrServerWrapper(new ExplanatoryHttpSolrServer("http://second.host/solr", mock(HttpClient.class))));
      servers
            .add(new SolrServerWrapper(new ExplanatoryHttpSolrServer("http://third.host/solr", mock(HttpClient.class))));

      strategy = new StickyDelegatingSolrLoadBalancerStrategy(new RoundRobinSolrLoadBalancerStrategy());
      RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
   }

   @After
   public void tearDown() {
      RequestContextHolder.resetRequestAttributes();
   }
   
   @Test
   public void testStickyness() throws Exception {
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
   }

   @Test
   public void testStickynessWithFailingServer() throws Exception {
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
      
      // server fails
      servers.get(0).setState(SolrServerState.DEAD);
      
      assertEquals(servers.get(2), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(2), strategy.nextAliveSolrServer(servers));
   }

   @Test
   public void testMissingStickynessWithMissingRequestAttributes() throws Exception {
      RequestContextHolder.resetRequestAttributes();
      
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(1), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(2), strategy.nextAliveSolrServer(servers));
      assertEquals(servers.get(0), strategy.nextAliveSolrServer(servers));
   }

}
