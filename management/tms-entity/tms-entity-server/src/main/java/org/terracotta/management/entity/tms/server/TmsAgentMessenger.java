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
package org.terracotta.management.entity.tms.server;

import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.voltron.proxy.ConcurrencyStrategy;
import org.terracotta.voltron.proxy.ExecutionStrategy;

import static org.terracotta.voltron.proxy.ExecutionStrategy.Location.BOTH;

/**
 * Interface backed by the @{{@link org.terracotta.entity.IEntityMessenger}} used to communicate a management call to execute on a server
 *
 * @author Mathieu Carbou
 */
public interface TmsAgentMessenger {

  @ConcurrencyStrategy(key = ConcurrencyStrategy.UNIVERSAL_KEY)
  @ExecutionStrategy(location = BOTH)
  void executeManagementCall(String managementCallIdentifier, ContextualCall<?> call);

}
