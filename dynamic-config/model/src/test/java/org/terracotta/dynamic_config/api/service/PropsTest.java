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
package org.terracotta.dynamic_config.api.service;

import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;
import java.nio.file.Paths;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Mathieu Carbou
 */
public class PropsTest {

  private final Properties properties = new Properties();

  @Before
  public void setUp() {
    // not ordered
    properties.setProperty("c", "c");
    properties.setProperty("b", "b");
    properties.setProperty("a", "a");
    properties.setProperty("d", "d");
  }

  @Test
  public void test_store_without_comment() {
    StringWriter sw = new StringWriter();
    Props.store(sw, properties, null);
    assertThat(sw.toString(), is(equalTo(
        "a=a\n" +
            "b=b\n" +
            "c=c\n" +
            "d=d"
    )));
  }

  @Test
  public void test_store_wit_empty_comment() {
    StringWriter sw = new StringWriter();
    Props.store(sw, properties, "");
    assertThat(sw.toString(), is(equalTo(
        "#\n" +
            "a=a\n" +
            "b=b\n" +
            "c=c\n" +
            "d=d"
    )));
  }

  @Test
  public void test_store_wit_a_comment() {
    StringWriter sw = new StringWriter();
    Props.store(sw, properties, "My Comment");
    assertThat(sw.toString(), is(equalTo(
        "#My Comment\n" +
            "a=a\n" +
            "b=b\n" +
            "c=c\n" +
            "d=d"
    )));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLoadForInvalidFileFormat() {
    Props.load(Paths.get("tc-config.xml"));
  }
}