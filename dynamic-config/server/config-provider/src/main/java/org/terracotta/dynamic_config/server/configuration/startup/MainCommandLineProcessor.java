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

import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.server.Server;

public class MainCommandLineProcessor implements CommandLineProcessor {
  private final Options options;
  private final ConfigurationGeneratorVisitor configurationGeneratorVisitor;
  private final ClusterFactory clusterCreator;
  private final IParameterSubstitutor parameterSubstitutor;
  private final Server server;

  public MainCommandLineProcessor(Options options,
                                  ClusterFactory clusterCreator,
                                  ConfigurationGeneratorVisitor configurationGeneratorVisitor,
                                  IParameterSubstitutor parameterSubstitutor,
                                  Server server) {
    this.options = options;
    this.clusterCreator = clusterCreator;
    this.configurationGeneratorVisitor = configurationGeneratorVisitor;
    this.parameterSubstitutor = parameterSubstitutor;
    this.server = server;
  }

  @Override
  public void process() {
    // Each NodeStarter either handles the startup itself or hands over to the next NodeStarter, following the chain-of-responsibility pattern
    CommandLineProcessor third = new ConsoleCommandLineProcessor(options, clusterCreator, configurationGeneratorVisitor, parameterSubstitutor, server);
    CommandLineProcessor second = new ConfigFileCommandLineProcessor(third, options, clusterCreator, configurationGeneratorVisitor, parameterSubstitutor, server);
    CommandLineProcessor first = new ConfigRepoCommandLineProcessor(second, options, configurationGeneratorVisitor, parameterSubstitutor, clusterCreator, server);
    first.process();
  }
}
