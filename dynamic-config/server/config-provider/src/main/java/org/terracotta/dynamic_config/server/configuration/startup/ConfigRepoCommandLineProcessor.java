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
package org.terracotta.dynamic_config.server.configuration.startup;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.server.Server;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.System.lineSeparator;
import static org.terracotta.common.struct.Tuple3.tuple3;

public class ConfigRepoCommandLineProcessor implements CommandLineProcessor {
  private final Options options;
  private final CommandLineProcessor nextStarter;
  private final ConfigurationGeneratorVisitor configurationGeneratorVisitor;
  private final IParameterSubstitutor parameterSubstitutor;
  private final ClusterFactory clusterCreator;
  private final Server server;

  ConfigRepoCommandLineProcessor(CommandLineProcessor nextStarter,
                                 Options options,
                                 ConfigurationGeneratorVisitor configurationGeneratorVisitor,
                                 IParameterSubstitutor parameterSubstitutor,
                                 ClusterFactory clusterCreator,
                                 Server server) {
    this.options = options;
    this.nextStarter = nextStarter;
    this.configurationGeneratorVisitor = configurationGeneratorVisitor;
    this.parameterSubstitutor = parameterSubstitutor;
    this.clusterCreator = clusterCreator;
    this.server = server;
  }

  @Override
  public void process() {
    Path configPath = configurationGeneratorVisitor.getOrDefaultConfigurationDirectory(options.getConfigDir());
    Optional<String> nodeName = configurationGeneratorVisitor.findNodeName(configPath, parameterSubstitutor);
    if (nodeName.isPresent()) {
      server.console("Found configuration directory at: {}. Other parameters will be ignored", parameterSubstitutor.substitute(configPath));

      // Build an alternate topology from the CLI in case we cannot load any config from the existing config repo.
      // This can happen in case a node is not properly activated
      Map<Setting, String> cliOptions = new LinkedHashMap<>(options.getTopologyOptions());
      Cluster cluster = clusterCreator.create(cliOptions, parameterSubstitutor);
      NodeContext alternate = new NodeContext(cluster, cluster.getSingleNode().get().getUID());

      configurationGeneratorVisitor.startUsingConfigRepo(configPath, nodeName.get(), options.wantsRepairMode(), alternate);

      // we are starting from a config repository, but the user might still have some old CLI from a previous run that has been discarded.
      {
        NodeContext loadedNodeContext = configurationGeneratorVisitor.getTopologyService().getUpcomingNodeContext();

        // creates a stream of comparisons to do for each setting
        cliOptions.keySet().stream()
            .map(setting -> tuple3(setting, alternate.getProperty(setting), loadedNodeContext.getProperty(setting)))
            .filter(tuple -> !Objects.equals(tuple.t2, tuple.t3))
            .map(mismatch -> " - Setting: '" + mismatch.t1 + "': CLI=" + mismatch.t2.orElse("<unspecified>") + " vs CONFIG=" + mismatch.t3.orElse("<unspecified>") + lineSeparator())
            .reduce(String::concat)
            .ifPresent(mismatches -> server.console(lineSeparator() + lineSeparator()
                + "=================================================================================================================" + lineSeparator()
                + "Node is starting from an existing configuration directory.                                                       " + lineSeparator()
                + "The following command-line (CLI) parameters differ from the loaded configuration.                                " + lineSeparator()
                + "They have been ignored and can be removed from the CLI.                                                          " + lineSeparator()
                + mismatches
                + "=================================================================================================================" + lineSeparator()
            ));
      }

      return;
    }

    // Couldn't start node - pass the responsibility to the next starter
    server.console("Did not find configuration directory at: {}", configPath);
    nextStarter.process();
  }
}
