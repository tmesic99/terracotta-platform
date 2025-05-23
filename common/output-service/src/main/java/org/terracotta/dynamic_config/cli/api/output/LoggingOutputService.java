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
package org.terracotta.dynamic_config.cli.api.output;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingOutputService implements OutputService {
  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingOutputService.class);

  @Override
  public void out(String format, Object... args) {
    LOGGER.info(format, args);
  }

  @Override
  public void info(String format, Object... args) {
    LOGGER.info(format, args);
  }

  @Override
  public void warn(String format, Object... args) {
    LOGGER.warn(format, args);
  }

  @Override
  public String toString() {
    return "logging";
  }
}
