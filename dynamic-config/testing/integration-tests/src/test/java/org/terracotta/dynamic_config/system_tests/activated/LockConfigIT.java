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

import org.junit.Test;
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class LockConfigIT extends DynamicConfigIT {
  private final LockContext lockContext =
      new LockContext(UUID.randomUUID().toString(), "platform", "dynamic-scale");

  @Test
  public void testLockUnlock() throws Exception {
    activate();
    lock();
    unlock();
  }

  @Test
  public void testSettingChangesWithoutTokenWhileLocked() throws Exception {
    activate();
    lock();

    assertThat(
        invokeWithoutToken("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.test=123MB"),
        containsOutput("changes are not allowed as config is locked by 'platform (dynamic-scale)'")
    );

    unlock();

    invokeWithoutToken("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.test=123MB");
  }

  @Test
  public void testSettingChangesWithTokenWhileLocked() throws Exception {
    activate();
    lock();

    invokeWithToken("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.test=123MB");
  }

  @Test
  public void testForceUnlock() throws Exception {
    activate();
    lock();

    invokeWithoutToken("repair", "-s", "localhost:" + getNodePort(), "-f", "unlock");

    invokeWithoutToken("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.test=123MB");
  }

  @Test
  public void testLockCanBeExported() throws Exception {
    activate();
    lock();
    assertThat(
        configTool("export", "-s", "localhost:" + getNodePort(), "-t", "properties"),
        containsOutput("lock-context=" + lockContext));
  }

  @Test
  public void testNodeCanJoinALockedClusterAndAlsoBeLocked() throws IOException {
    // auto activating a stripe
    // The goal is to have an activated cluster with inside its topology some "room" to add a node that is not yet created
    // this situation can happen in case of node failure we need to replace, when auto-activating at startup, etc.
    Path configurationFile = copyConfigProperty("/config-property-files/1x2.properties");
    startNode(1, 1, "--auto-activate", "-f", configurationFile.toString(), "-s", "localhost", "-p", String.valueOf(getNodePort(1, 1)), "--config-dir", getBaseDir().resolve("node-1-1").toString());
    waitForActive(1, 1);

    // we lock the configuration
    // but not all nodes are there
    if (!lock()) {
      throw new AssertionError("lock failed");
    }

    // we need to repair / or make a node join
    // we will be able to add it through a restrictive activation
    // For a node to be able to join a topology, it needs to have EXACTLY the same topology information of the target cluster
    Path exportedConfigPath = tmpDir.getRoot().resolve("cluster.properties").toAbsolutePath();
    assertThat(configTool("export", "-s", "localhost:" + getNodePort(1, 1), "-f", exportedConfigPath.toString(), "-t", "properties"), is(successful()));
    //System.out.println(new String(Files.readAllBytes(exportedConfigPath), StandardCharsets.UTF_8));
    assertThat(Props.toString(Props.load(exportedConfigPath)), Props.load(exportedConfigPath).stringPropertyNames(), hasItem("lock-context"));

    startNode(1, 2);

    assertThat(
        configTool("activate", "-R", "-s", "localhost:" + getNodePort(1, 2), "-f", exportedConfigPath.toString()),
        allOf(containsOutput("No license specified for activation"), containsOutput("came back up")));
  }

  @Test
  public void testLockContextCliUsage() throws Exception {
    activate();
    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "lock-context=" + lockContext),
        containsOutput("'lock-context' is not supported")
    );
    assertThat(
        configTool("unset", "-s", "localhost:" + getNodePort(), "-c", "lock-context"),
        containsOutput("'lock-context' is not supported")
    );
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "lock-context"),
        containsOutput("'lock-context' is not supported")
    );
  }

  private boolean lock() {
    ToolExecutionResult result = invokeWithoutToken("lock-config", "-s", "localhost:" + getNodePort(), "--lock-context", lockContext.toString());
    if (result.getExitStatus() != 0) {
      for (String line : result.getOutput()) {
        System.out.println("LOCK FAIL:" + line);
      }
      return false;
    } else {
      return true;
    }
  }

  private void unlock() {
    invokeWithToken("unlock-config", "-s", "localhost:" + getNodePort());
  }

  private ToolExecutionResult invokeWithoutToken(String... args) {
    return configTool(args);
  }

  private void invokeWithToken(String... args) {
    List<String> newArgs = new ArrayList<>(asList("--lock-token", lockContext.getToken()));
    newArgs.addAll(asList(args));
    assertThat(configTool(newArgs.toArray(new String[0])), is(successful()));
  }

  private void activate() {
    startNode(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start the second node
    startNode(1, 2);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(1)));

    //attach the second node
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));

    //Activate cluster
    activateCluster();
    waitForActive(1);
    waitForNPassives(1, 1);
  }
}
