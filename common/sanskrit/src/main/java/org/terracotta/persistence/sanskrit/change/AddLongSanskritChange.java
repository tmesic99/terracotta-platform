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
package org.terracotta.persistence.sanskrit.change;

import org.terracotta.persistence.sanskrit.SanskritException;

/**
 * A data change corresponding to adding a mapping between a (String) key and a long value.
 */
public class AddLongSanskritChange implements SanskritChange {
  private final String key;
  private final long value;

  public AddLongSanskritChange(String key, long value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public void accept(SanskritChangeVisitor visitor) throws SanskritException {
    visitor.setLong(key, value);
  }
}
