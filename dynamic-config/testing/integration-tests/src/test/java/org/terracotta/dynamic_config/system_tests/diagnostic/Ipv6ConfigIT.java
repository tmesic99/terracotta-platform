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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.ConfigurationGenerator;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false, failoverPriority = "")
public class Ipv6ConfigIT extends DynamicConfigIT {

  @Test
  public void testStartupFromConfigFileAndExportCommand() {
    Path file = tmpDir.getRoot().resolve("output.json");

    Path configurationFile = copyConfigProperty("/config-property-files/single-stripe_multi-node_ipv6.properties");
    startNode(1, 1, "-f", configurationFile.toString(), "-s", "[::1]", "-p", String.valueOf(getNodePort()), "-r", "config");

    assertThat(configTool("export", "-s", "[::1]:" + getNodePort(), "-f", file.toString(), "-t", "json"), is(successful()));
    stopNode(1, 1);

    startNode(1, 1, "-f", configurationFile.toString(), "-s", "::1", "-p", String.valueOf(getNodePort()), "-r", "config");
    waitForDiagnostic(1, 1);
  }

  @Test
  public void testStartupFromMigratedConfigRepoAndGetCommand() throws Exception {
    Path configurationRepo = generateNodeConfigDir(1, 1, ConfigurationGenerator::generate1Stripe1NodeIpv6);
    startNode(1, 1, "--config-dir", configurationRepo.toString());
    waitForActive(1, 1);

    assertThat(
        configTool("get", "-s", "[::1]:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=512MB"));
  }
}
