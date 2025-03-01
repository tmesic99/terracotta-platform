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
package org.terracotta.json;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * @author Mathieu Carbou
 */
public class PathTest {

  @Test
  public void test_new_path_mapping() {
    Json json = new DefaultJsonFactory().create();

    assertThat(json.toString(new Foo()), is(equalTo("{}")));
    assertThat(json.toString(new Foo().setPath(Paths.get(""))), is(equalTo("{\"path\":[\"\"]}")));
    assertThat(json.toString(new Foo().setPath(Paths.get("foo"))), is(equalTo("{\"path\":[\"foo\"]}")));
    assertThat(json.toString(new Foo().setPath(Paths.get("foo", "bar"))), is(equalTo("{\"path\":[\"foo\",\"bar\"]}")));

    assertThat(json.parse("{\"path\":null}", Foo.class), is(equalTo(new Foo())));
    assertThat(json.parse("{\"path\":[\"\"]}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("")))));
    assertThat(json.parse("{\"path\":[\"foo\"]}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo")))));
    assertThat(json.parse("{\"path\":[\"foo\",\"bar\"]}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo", "bar")))));
  }

  @Test
  public void test_new_path_mapping_backward_compatible() {
    Json json = new DefaultJsonFactory().create();

    assertThat(json.parse("{\"path\":null}", Foo.class), is(equalTo(new Foo())));
    assertThat(json.parse("{\"path\":\"\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("")))));
    assertThat(json.parse("{\"path\":\"foo\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo")))));
    assertThat(json.parse("{\"path\":\"foo/bar\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo", "bar")))));
    if (isWindows()) {
      assertThat(json.parse("{\"path\":\"foo\\\\bar\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo", "bar")))));
    } else {
      assertThat(json.parse("{\"path\":\"foo\\\\bar\"}", Foo.class), is(equalTo(new Foo().setPath(Paths.get("foo\\bar")))));
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }

  public static class Foo {
    private Path path;

    public Foo setPath(Path path) {
      this.path = path;
      return this;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Foo)) return false;
      Foo foo = (Foo) o;
      return Objects.equals(path, foo.path);
    }

    @Override
    public int hashCode() {
      return Objects.hash(path);
    }

    @Override
    public String toString() {
      if (path == null) {
        return "null";
      }
      List<String> segments = new ArrayList<>(path.getNameCount());
      for (Path p : path) {
        segments.add(p.toString());
      }
      return segments.toString();
    }
  }
}
