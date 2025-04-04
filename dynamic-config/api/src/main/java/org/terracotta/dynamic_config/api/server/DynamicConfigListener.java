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
package org.terracotta.dynamic_config.api.server;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeState;

/**
 * @author Mathieu Carbou
 */
public interface DynamicConfigListener extends DynamicConfigEventFiring {

  /**
   * Listener that will be called when a new configuration has been stored on disk, which happens in Nomad PREPARE phase
   * <p>
   * The method is called with the future topology equivalent to {@link TopologyService#getUpcomingNodeContext()} that will be applied after restart
   * <p>
   * All the nodes are called during PREPARE to save a new configuration, regardless of this applicability level.
   * So this listener will be called on every node.
   */
  default void onNewConfigurationSaved(NodeContext nodeContext, Long version) {}

  /**
   * Listener that will be called when a new configuration has been applied at runtime on a server, through a {@link ConfigChangeHandler}
   * <p>
   * The method is called with the topology equivalent to {@link TopologyService#getRuntimeNodeContext()} or
   * {@link TopologyService#getUpcomingNodeContext()} ()} and the change that has been applied, depending on whether the change requires a restart or not
   * <p>
   * Only the nodes targeted by the applicability filter will be called through this listener after the {@link ConfigChangeHandler} is called
   */
  default void onSettingChanged(SettingNomadChange change, Cluster updated) {}

  /**
   * Listener that will be called when some nodes have been removed from a stripe
   * <p>
   * The method is called with the topology equivalent to {@link TopologyService#getRuntimeNodeContext()} and the change that has been applied
   * <p>
   * Only the nodes targeted by the applicability filter will be called through this listener after the {@link ConfigChangeHandler} is called
   *
   * @param removedNode the details about the removed node
   */
  default void onNodeRemoval(UID stripeUID, Node removedNode) {}

  /**
   * Listener that will be called when some nodes have been added to a stripe
   * <p>
   * The method is called with the topology equivalent to {@link TopologyService#getRuntimeNodeContext()} and the change that has been applied
   * <p>
   * Only the nodes targeted by the applicability filter will be called through this listener after the {@link ConfigChangeHandler} is called
   *
   * @param stripeUID the stripe UID where the nodes have been added
   * @param addedNode the details of the added node
   */
  default void onNodeAddition(UID stripeUID, Node addedNode) {}

  default void onNomadPrepare(PrepareMessage message, AcceptRejectResponse response) {}

  default void onNomadCommit(CommitMessage message, AcceptRejectResponse response, ChangeState<NodeContext> changeState) {}

  default void onNomadRollback(RollbackMessage message, AcceptRejectResponse response) {}

  default void onStripeAddition(Stripe addedStripe) {}

  default void onStripeRemoval(Stripe removedStripe) {}
}
