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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.terracotta.dynamic_config.api.model.NodeContext;

/**
 * @author Mathieu Carbou
 */
public class ConfigStorageAdapter implements ConfigStorage {

  private final ConfigStorage delegate;

  public ConfigStorageAdapter(ConfigStorage delegate) {
    this.delegate = delegate;
  }

  @Override
  public Config getConfig(long version) throws ConfigStorageException {return delegate.getConfig(version);}

  @Override
  public void saveConfig(long version, NodeContext config) throws ConfigStorageException {delegate.saveConfig(version, config);}

  @Override
  public void reset() throws ConfigStorageException {
    delegate.reset();
  }
}
