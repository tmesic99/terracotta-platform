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
package org.terracotta.diagnostic.server;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.diagnostic.server.api.DiagnosticServicesRegistration;
import org.terracotta.diagnostic.server.api.Expose;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.server.ServerJMX;

import javax.management.MBeanServerFactory;
import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultDiagnosticServicesTest {

  @Spy MyService1Impl service1;
  @Spy MyServiceImpl service2;

  ServerJMX jmx = mock(ServerJMX.class);
  DefaultDiagnosticServices diagnosticServices = new DefaultDiagnosticServices(jmx, new DefaultJsonFactory());

  @Before
  public void setUp() {
    when(jmx.getMBeanServer()).thenReturn(MBeanServerFactory.newMBeanServer());
    diagnosticServices.init();
  }

  @After
  public void tearDown() {
    diagnosticServices.close();
    reset(service1, service2);
  }

  @Test
  public void test_register() {
    assertThat(
        () -> diagnosticServices.register(null, service1),
        is(throwing(instanceOf(NullPointerException.class))));
    assertThat(
        () -> diagnosticServices.register(MyService1.class, null),
        is(throwing(instanceOf(NullPointerException.class))));

    assertThat(diagnosticServices.register(MyService1.class, service1), is(instanceOf(DiagnosticServicesRegistration.class)));
    assertThat(
        () -> diagnosticServices.register(MyService1.class, service1),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Service org.terracotta.diagnostic.server.DefaultDiagnosticServicesTest$MyService1 is already registered")))));
    assertThat(diagnosticServices.register(MyService2.class, service2), is(instanceOf(DiagnosticServicesRegistration.class)));
  }

  @Test
  public void test_unregister() {
    assertThat(
        () -> diagnosticServices.unregister(null),
        is(throwing(instanceOf(NullPointerException.class))));

    // does not crash when unregister inexisting service
    diagnosticServices.unregister(Closeable.class);

    assertThat(diagnosticServices.register(MyService1.class, service1), is(instanceOf(DiagnosticServicesRegistration.class)));
    assertThat(diagnosticServices.findService(MyService1.class).isPresent(), is(true));
    diagnosticServices.unregister(MyService1.class);
    assertThat(diagnosticServices.findService(MyService1.class).isPresent(), is(false));
  }

  @Test
  public void test_findService() {
    assertThat(
        () -> diagnosticServices.findService(null),
        is(throwing(instanceOf(NullPointerException.class))));

    assertThat(diagnosticServices.findService(MyService1.class).isPresent(), is(false));

    assertThat(diagnosticServices.register(MyService1.class, service1), is(instanceOf(DiagnosticServicesRegistration.class)));
    assertThat(diagnosticServices.findService(MyService1.class).isPresent(), is(true));

    assertThat(diagnosticServices.findService(MyService2.class).isPresent(), is(false));
  }

  @Test
  public void test_async_support() {
    AtomicInteger i = new AtomicInteger();
    diagnosticServices.onService(MyService1.class).thenRun(i::incrementAndGet);
    assertThat(i.get(), is(equalTo(0)));

    diagnosticServices.register(MyService1.class, service1);
    assertThat(i.get(), is(equalTo(1)));

    diagnosticServices.onService(MyService1.class).thenRun(i::incrementAndGet);
    assertThat(i.get(), is(equalTo(2)));
  }

  public interface MyService1 {
    String say1(String word);
  }

  public static class MyService1Impl implements MyService1 {
    @Override
    public String say1(String word) {
      return "1. Hello " + word;
    }
  }

  public interface MyService2 {
    String say2(String word);
  }

  @Expose("s2")
  public static class MyServiceImpl implements MyService2 {
    @Override
    public String say2(String word) {
      return "2. Hello " + word;
    }
  }

}
