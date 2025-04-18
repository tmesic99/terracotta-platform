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
package org.terracotta.dynamic_config.server.service;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.server.NomadPermissionChangeProcessor;
import org.terracotta.nomad.server.NomadException;

import static org.terracotta.dynamic_config.api.model.ClusterState.ACTIVATED;

/**
 * @author Mathieu Carbou
 */
class DisallowSettingChanges implements NomadPermissionChangeProcessor.Check {
  @Override
  public void check(NodeContext config, DynamicConfigNomadChange change) throws NomadException {
    if (change instanceof SettingNomadChange) {
      Setting setting = ((SettingNomadChange) change).getSetting();
      if (!setting.isWritableWhen(ACTIVATED)) {
        throw new NomadException("Error when applying setting change: '" + change.getSummary() + "': " + "Setting '" + setting + "' cannot be changed once a node is activated");
      }
    }
  }
}
