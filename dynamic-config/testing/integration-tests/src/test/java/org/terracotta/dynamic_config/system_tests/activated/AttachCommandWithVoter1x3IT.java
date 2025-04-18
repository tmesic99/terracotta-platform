/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.voter.VotingGroup;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 3)
public class AttachCommandWithVoter1x3IT extends DynamicConfigIT {

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency(1);
  }

  @Before
  public void setUp() throws Exception {
    startNode(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start the second node
    startNode(1, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    //attach the second node
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    //Activate cluster
    activateCluster();
  }

  @Test
  public void testAttachAndVerifyWithVoter() throws Exception {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    try (VotingGroup activeVoter = new VotingGroup("mvoter", new Properties(), getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort())) {
      activeVoter.start();

      startNode(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

      String[] nodes = new String[]{
          getNode(1, activeId).getHostPort(),
          getNode(1, passiveId).getHostPort(),
          getNode(1, 3).getHostPort()};

      waitUntil(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      waitUntil(() -> activeVoter.countConnectedServers(), is(3));

      // kill the old passive and detach it from cluster
      stopNode(1, passiveId);

      assertThat(configTool("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));
      activeVoter.forceTopologyUpdate().join();
      nodes = new String[]{
          getNode(1, activeId).getHostPort(),
          getNode(1, 3).getHostPort()};

      waitUntil(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      waitUntil(() -> activeVoter.countConnectedServers(), is(2));

      withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    }
  }

  @Test
  public void testAttachAfterKillingActive() throws Exception {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    try (VotingGroup activeVoter = new VotingGroup("mvoter", new Properties(), getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort())) {
      activeVoter.start();

      startNode(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      //Kill active so other passive becomes active by the vote of voter
      stopNode(1, activeId);
      waitForActive(1, passiveId);

      assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));
      activeVoter.forceTopologyUpdate().join();
      String[] nodes = new String[]{
          getNode(1, activeId).getHostPort(),
          getNode(1, passiveId).getHostPort(),
          getNode(1, 3).getHostPort()};

      waitUntil(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      waitUntil(() -> activeVoter.countConnectedServers(), is(2));
    }
  }

  @Test
  public void testStalePassivePortsRemovedFromVoterTopology() throws Exception {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    try (VotingGroup activeVoter = new VotingGroup("mvoter", new Properties(), getNode(1, activeId).getHostPort(), "localhost:123", "locahost:235")) {
      // Adding some dummy passive hostPorts to simulate as stale passive hostPorts
      activeVoter.start();

      startNode(1, 3);
      assertThat(getUpcomingCluster("localhost", getNodePort(1, 3)).getNodeCount(), is(equalTo(1)));

      assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 3)), is(successful()));

      String[] nodes = new String[]{
          getNode(1, activeId).getHostPort(),
          getNode(1, passiveId).getHostPort(),
          getNode(1, 3).getHostPort()};
      activeVoter.forceTopologyUpdate().join();
      waitUntil(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      waitUntil(() -> activeVoter.countConnectedServers(), is(3));

      withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, 3, topologyService -> assertTrue(topologyService.isActivated()));
    }
  }
}
