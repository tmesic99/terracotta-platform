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
package org.terracotta.diagnostic.client.connection;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConcurrencySizingTest {
  @Test
  public void small() {
    assertEquals(10, new ConcurrencySizing().getThreadCount(10));
  }

  @Test
  public void large() {
    assertEquals(64, new ConcurrencySizing().getThreadCount(100));
  }
}
