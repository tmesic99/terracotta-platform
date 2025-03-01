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
package org.terracotta.offheapresource;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.fail;

public class OffHeapResourceIdentifierTest {

  @Test
  public void testNullName() {
    try {
      OffHeapResourceIdentifier.identifier(null);
      fail("Expected NullPointerException");
    } catch (NullPointerException e) {
      //expected
    }
  }

  @Test
  public void testServiceType() {
    assertThat(OffHeapResourceIdentifier.identifier("foo").getServiceType(), equalTo(OffHeapResource.class));
  }

  @Test
  public void testEquals() {
    assertThat(OffHeapResourceIdentifier.identifier("foo").equals(OffHeapResourceIdentifier.identifier("foo")), is(true));
    assertThat(OffHeapResourceIdentifier.identifier("foo").equals(OffHeapResourceIdentifier.identifier("bar")), is(false));
  }

  @Test
  public void testHashcode() {
    assertThat(OffHeapResourceIdentifier.identifier("foo").hashCode(), is(OffHeapResourceIdentifier.identifier("foo").hashCode()));
  }
}
