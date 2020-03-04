/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.dynamic_config.server.startup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.service.ParameterSubstitutor;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigFileStarter implements NodeStarter {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFileStarter.class);
  private static final IParameterSubstitutor PARAMETER_SUBSTITUTOR = new ParameterSubstitutor();

  private final Options options;
  private final ClusterFactory clusterCreator;
  private final NodeStarter nextStarter;
  private final StartupManager startupManager;

  ConfigFileStarter(Options options, ClusterFactory clusterCreator,
                    StartupManager startupManager, NodeStarter nextStarter) {
    this.options = options;
    this.clusterCreator = clusterCreator;
    this.nextStarter = nextStarter;
    this.startupManager = startupManager;
  }

  @Override
  public boolean startNode() {
    if (options.getConfigFile() == null) {
      // If config file wasn't specified - pass the responsibility to the next starter
      return nextStarter.startNode();
    }

    Path substitutedConfigFile = Paths.get(PARAMETER_SUBSTITUTOR.substitute(options.getConfigFile()));
    LOGGER.info("Starting node from config file: {}", substitutedConfigFile);
    Cluster cluster = clusterCreator.create(substitutedConfigFile);

    // overwrite the cluster name if given in CLI on top of within the config file
    if (options.getClusterName() != null) {
      cluster.setName(options.getClusterName());
    }

    Node node = startupManager.getMatchingNodeFromConfigFile(options.getNodeHostname(), options.getNodePort(), options.getConfigFile(), cluster);

    if (cluster.getName() != null) {
      if (cluster.getStripeCount() > 1) {
        throw new UnsupportedOperationException("Cannot start a pre-activated multi-stripe cluster");
      }
      return startupManager.startActivated(cluster, node, options.getLicenseFile(), options.getNodeRepositoryDir());
    } else {
      return startupManager.startUnconfigured(cluster, node, options.getNodeRepositoryDir());
    }
  }
}