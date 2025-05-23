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
package org.terracotta.diagnostic.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class JavaDiagnosticCodec extends DiagnosticCodecSkeleton<byte[]> {
  public JavaDiagnosticCodec() {
    super(byte[].class);
  }

  @Override
  public byte[] serialize(Object o) throws DiagnosticCodecException {
    requireNonNull(o);
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream oos = new ObjectOutputStream(baos)) {
      oos.writeObject(o);
      oos.flush();
      return baos.toByteArray();
    } catch (Exception e) {
      throw new DiagnosticCodecException(e);
    }
  }

  @Override
  public <T> T deserialize(byte[] encoded, Class<T> target) throws DiagnosticCodecException {
    requireNonNull(encoded);
    requireNonNull(target);
    try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(encoded))) {
      return target.cast(ois.readObject());
    } catch (Exception e) {
      throw new DiagnosticCodecException(e);
    }
  }

  @Override
  public String toString() {
    return "Java";
  }
}
