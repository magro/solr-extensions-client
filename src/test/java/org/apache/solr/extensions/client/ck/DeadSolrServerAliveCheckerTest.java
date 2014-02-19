package org.apache.solr.extensions.client.ck;


import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.List;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.extensions.client.SolrServerWrapper;
import org.apache.solr.extensions.client.SolrServerWrapper.SolrServerState;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;

public class DeadSolrServerAliveCheckerTest {

   private DeadSolrServerAliveChecker checker;
   private List<SolrServerWrapper> servers;

   @Before
   public void setUp() throws Exception {
      checker = new DeadSolrServerAliveChecker(6);

      servers = Lists.newArrayList();

      servers.add(new SolrServerWrapper(mock(HttpSolrServer.class)));
      servers.add(new SolrServerWrapper(mock(HttpSolrServer.class)));
      servers.add(new SolrServerWrapper(mock(HttpSolrServer.class)));

   }

   @Test
   public void testCheckInterval() throws Exception {
      assertEquals(6, checker.getCheckIntervalInSeconds());
   }

   @Test
   public void testRoundRobinPingForAllDeadServers() throws Exception {
      servers.get(1).setState(SolrServerState.DEAD);
      servers.get(2).setState(SolrServerState.DEAD);
      
      checker.check(servers);
      
      assertEquals(SolrServerState.ALIVE, servers.get(0).getState());
      verifyZeroInteractions(servers.get(0).getServer());
      assertEquals(SolrServerState.ALIVE, servers.get(1).getState());
      verify(servers.get(1).getServer(), times(1)).ping();
      assertEquals(SolrServerState.ALIVE, servers.get(2).getState());
      verify(servers.get(2).getServer(), times(1)).ping();
   }
}
