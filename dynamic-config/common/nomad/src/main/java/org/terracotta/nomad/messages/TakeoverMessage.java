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
package org.terracotta.nomad.messages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public class TakeoverMessage extends MutativeMessage {
  @JsonCreator
  public TakeoverMessage(@JsonProperty(value = "expectedMutativeMessageCount", required = true) long expectedMutativeMessageCount,
                         @JsonProperty(value = "mutationHost", required = true) String mutationHost,
                         @JsonProperty(value = "mutationUser", required = true) String mutationUser,
                         @JsonProperty(value = "mutationTimestamp", required = true) Instant mutationTimestamp) {
    super(expectedMutativeMessageCount, mutationHost, mutationUser, mutationTimestamp);
  }

  @Override
  public String toString() {
    return "TakeoverMessage{" +
        "expectedMutativeMessageCount=" + getExpectedMutativeMessageCount() +
        ", mutationHost='" + getMutationHost() + '\'' +
        ", mutationUser='" + getMutationUser() + '\'' +
        ", mutationTimestamp=" + getMutationTimestamp() +
        '}';
  }
}
