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
package org.terracotta.dynamic_config.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tc.classloader.CommonComponent;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@CommonComponent
public final class License {

  // Mapping between capability name and corresponding limit value
  // Value as 0 means absent and > 0 means present and represents its limit.
  private final Map<String, Long> capabilityLimitMap;

  // Expiry date of the license in UTC.
  private final LocalDate expiryDate;

  @JsonCreator
  public License(@JsonProperty(value = "capabilities", required = true) Map<String, Long> capabilityLimitMap,
                 @JsonProperty(value = "expiryDate", required = true) LocalDate expiryDate) {
    this.capabilityLimitMap = Collections.unmodifiableMap(new HashMap<>(capabilityLimitMap));
    this.expiryDate = expiryDate;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public Map<String, Long> getCapabilityLimitMap() {
    return capabilityLimitMap;
  }

  public boolean hasCapability(String capability) {
    Long v = getLimit(capability);
    return v != null && v != 0;
  }

  public Long getLimit(String capability) {
    return capabilityLimitMap.get(capability);
  }

  @Override
  public String toString() {
    return "License{" +
        "capabilityLimitMap=" + capabilityLimitMap +
        ", expiryDate=" + expiryDate +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    License license = (License) o;
    return capabilityLimitMap.equals(license.capabilityLimitMap) && expiryDate.equals(license.expiryDate);
  }

  @Override
  public int hashCode() {
    return 31 * capabilityLimitMap.hashCode() + expiryDate.hashCode();
  }
}
