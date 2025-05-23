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
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;

import java.util.Objects;

public class LockAwareDynamicConfigNomadChange implements DynamicConfigNomadChange {
  private final String lockToken;
  private final DynamicConfigNomadChange change;

  // For Json
  LockAwareDynamicConfigNomadChange() {
    lockToken = null;
    change = null;
  }

  public LockAwareDynamicConfigNomadChange(String lockToken, DynamicConfigNomadChange change) {
    this.lockToken = lockToken;
    this.change = change;
  }

  @Override
  public Cluster apply(Cluster original) {
    return change.apply(original);
  }

  @Override
  public boolean canUpdateRuntimeTopology(NodeContext currentNode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getSummary() {
    return change.getSummary();
  }

  public String getLockToken() {
    return lockToken;
  }

  public DynamicConfigNomadChange getChange() {
    return change;
  }

  @Override
  public DynamicConfigNomadChange unwrap() {
    return change;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final LockAwareDynamicConfigNomadChange that = (LockAwareDynamicConfigNomadChange) o;
    return Objects.equals(lockToken, that.lockToken) &&
        Objects.equals(change, that.change);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lockToken, change);
  }
}
