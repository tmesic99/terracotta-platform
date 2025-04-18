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
package org.terracotta.management.service.monitoring.registry.provider;

import java.util.Objects;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.cluster.ClientIdentifier;

public class ClientBinding {

  private final ClientDescriptor clientDescriptor;
  private final Object value;

  volatile ClientIdentifier resolvedClientIdentifier;

  public ClientBinding(ClientDescriptor clientDescriptor) {
    this(clientDescriptor, null);
  }

  public ClientBinding(ClientDescriptor clientDescriptor, Object value) {
    this.clientDescriptor = Objects.requireNonNull(clientDescriptor);
    this.value = value;
  }

  public ClientDescriptor getClientDescriptor() {
    return clientDescriptor;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ClientBinding that = (ClientBinding) o;
    return clientDescriptor.equals(that.clientDescriptor);
  }

  @Override
  public int hashCode() {
    return clientDescriptor.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");
    sb.append("clientDescriptor=").append(clientDescriptor);
    sb.append(", value=").append(value);
    sb.append('}');
    return sb.toString();
  }

}
