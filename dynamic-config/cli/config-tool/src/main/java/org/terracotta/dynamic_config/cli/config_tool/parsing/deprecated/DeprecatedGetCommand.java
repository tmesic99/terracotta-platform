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
package org.terracotta.dynamic_config.cli.config_tool.parsing.deprecated;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.BooleanConverter;
import org.terracotta.dynamic_config.api.model.ConfigFormat;
import org.terracotta.dynamic_config.cli.api.command.ConfigurationInput;
import org.terracotta.dynamic_config.cli.api.command.GetAction;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.command.Command;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.converter.ConfigFormatConverter;
import org.terracotta.dynamic_config.cli.converter.ConfigurationInputConverter;
import org.terracotta.dynamic_config.cli.converter.HostPortConverter;
import org.terracotta.dynamic_config.cli.converter.MultiConfigCommaSplitter;
import org.terracotta.inet.HostPort;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Parameters(commandDescription = "Read configuration properties")
@Usage("-s <hostname[:port]> [-r] [-t cfg|properties] -c <[namespace:]property> -c <[namespace:]property> ...")
public class DeprecatedGetCommand extends Command {

  @Parameter(names = {"-s"}, description = "Node to connect to", required = true, converter = HostPortConverter.class)
  HostPort node;

  @Parameter(names = {"-c"}, description = "Configuration properties", splitter = MultiConfigCommaSplitter.class, required = true, converter = ConfigurationInputConverter.class)
  List<ConfigurationInput> inputs;

  @Parameter(names = {"-r"}, description = "Read the properties from the current runtime configuration instead of reading them from the last configuration saved on disk", converter = BooleanConverter.class)
  private boolean wantsRuntimeConfig;

  @Parameter(names = {"-t"}, description = "Output format (cfg|properties). Default: cfg", converter = ConfigFormatConverter.class)
  private ConfigFormat outputFormat = ConfigFormat.CONFIG;

  @Inject
  public GetAction action;

  public DeprecatedGetCommand() {
    this(new GetAction());
  }

  public DeprecatedGetCommand(GetAction action) {
    this.action = action;
  }

  @Override
  public void run() {
    action.setNodes(node == null ? emptyList() : singletonList(node));
    action.setConfigurationInputs(inputs);
    action.setRuntimeConfig(wantsRuntimeConfig);
    action.setOutputFormat(outputFormat);

    action.run();
  }
}
