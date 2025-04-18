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
package org.terracotta.dynamic_config.cli.api;

import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.connection.ConnectionException;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.connection.ConcurrencySizing;
import org.terracotta.diagnostic.client.connection.ConcurrentDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DefaultDiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.DiagnosticServiceProvider;
import org.terracotta.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import org.terracotta.dynamic_config.api.json.DynamicConfigJsonModule;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.cli.api.json.TestModule;
import org.terracotta.dynamic_config.cli.api.nomad.DefaultNomadManager;
import org.terracotta.dynamic_config.cli.api.nomad.NomadManager;
import org.terracotta.dynamic_config.cli.api.output.ConsoleOutputService;
import org.terracotta.dynamic_config.cli.api.output.OutputService;
import org.terracotta.dynamic_config.cli.api.restart.RestartService;
import org.terracotta.dynamic_config.cli.api.stop.StopService;
import org.terracotta.inet.HostPort;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.entity.client.NomadEntity;
import org.terracotta.nomad.entity.client.NomadEntityProvider;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class BaseTest {

  protected DiagnosticServiceProvider diagnosticServiceProvider;
  protected MultiDiagnosticServiceProvider multiDiagnosticServiceProvider;
  protected NomadEntityProvider nomadEntityProvider;
  protected NomadManager<NodeContext> nomadManager;
  protected RestartService restartService;
  protected StopService stopService;
  protected ConcurrencySizing concurrencySizing = new ConcurrencySizing();
  protected Json.Factory jsonFactory = new DefaultJsonFactory()
      .withModule(new DynamicConfigJsonModule())
      .withModule(new TestModule());
  protected Json json = jsonFactory.create();
  protected OutputService outputService;

  private final Cache<HostPort, TopologyService> topologyServices = new Cache<>(addr -> mock(TopologyService.class, addr.toString()));

  private final Cache<HostPort, DynamicConfigService> dynamicConfigServices = new Cache<>(addr -> mock(DynamicConfigService.class, addr.toString()));

  @SuppressWarnings({"unchecked", "rawtypes"})
  private final Cache<HostPort, NomadServer<NodeContext>> nomadServers = new Cache<>(addr -> mock(NomadServer.class, addr.toString()));

  private final Cache<HostPort, DiagnosticService> diagnosticServices = new Cache<>(addr -> {
    final DiagnosticService diagnosticService = mock(DiagnosticService.class, addr.toString());
    lenient().when(diagnosticService.getProxy(TopologyService.class)).thenAnswer(invocation -> topologyServices.get(addr));
    lenient().when(diagnosticService.getProxy(DynamicConfigService.class)).thenAnswer(invocation -> dynamicConfigServices.get(addr));
    lenient().when(diagnosticService.getProxy(NomadServer.class)).thenAnswer(invocation -> nomadServers.get(addr));
    return diagnosticService;
  });

  private final Cache<Collection<HostPort>, NomadEntity<?>> nomadEntities = new Cache<>(list -> {
    final NomadEntity<?> entity = mock(NomadEntity.class, list.toString());
    try {
      lenient().when(entity.commit(any(CommitMessage.class))).thenReturn(AcceptRejectResponse.accept());
    } catch (NomadException e) {
      throw new AssertionError(e);
    }
    return entity;
  });

  @Rule
  public Timeout timeout = Timeout.builder().withLookingForStuckThread(true).withTimeout(1, TimeUnit.MINUTES).build();

  @Before
  public void setUp() throws Exception {
    Duration timeout = Duration.ofSeconds(2);
    diagnosticServiceProvider = new DefaultDiagnosticServiceProvider(getClass().getSimpleName(), timeout, timeout, null, jsonFactory) {
      @Override
      public DiagnosticService fetchDiagnosticService(InetSocketAddress address, Duration timeout) {
        return diagnosticServices.get(HostPort.create(address));
      }
    };
    nomadEntityProvider = new NomadEntityProvider(getClass().getSimpleName(), timeout, new NomadEntity.Settings().setRequestTimeout(timeout), null) {
      @SuppressWarnings("unchecked")
      @Override
      public <T> NomadEntity<T> fetchNomadEntity(Collection<InetSocketAddress> addresses) throws ConnectionException {
        return (NomadEntity<T>) nomadEntities.get(addresses.stream().map(HostPort::create).collect(Collectors.toList()));
      }
    };
    multiDiagnosticServiceProvider = new ConcurrentDiagnosticServiceProvider(diagnosticServiceProvider, timeout, new ConcurrencySizing());
    nomadManager = new DefaultNomadManager<>(new NomadEnvironment(), multiDiagnosticServiceProvider, nomadEntityProvider);
    restartService = new RestartService(diagnosticServiceProvider, concurrencySizing);
    stopService = new StopService(diagnosticServiceProvider, concurrencySizing);
    outputService = new ConsoleOutputService();
  }

  protected DiagnosticService diagnosticServiceMock(String host, int port) {
    return diagnosticServiceMock(HostPort.create(host, port));
  }

  protected DiagnosticService diagnosticServiceMock(HostPort address) {
    return diagnosticServices.get(address);
  }

  protected TopologyService topologyServiceMock(HostPort address) {
    return topologyServices.get(address);
  }

  protected TopologyService topologyServiceMock(String host, int port) {
    return topologyServiceMock(HostPort.create(host, port));
  }

  protected DynamicConfigService dynamicConfigServiceMock(HostPort address) {
    return dynamicConfigServices.get(address);
  }

  protected DynamicConfigService dynamicConfigServiceMock(String host, int port) {
    return dynamicConfigServiceMock(HostPort.create(host, port));
  }

  protected NomadServer<NodeContext> nomadServerMock(String host, int port) {
    return nomadServers.get(HostPort.create(host, port));
  }

  /**
   * @author Mathieu Carbou
   */
  private static class Cache<K, V> {
    private final Function<K, V> loader;
    private final Map<K, V> cache = new ConcurrentHashMap<>();

    Cache(Function<K, V> loader) {
      this.loader = loader;
    }

    public V get(K key) {
      return cache.computeIfAbsent(key, loader);
    }
  }
}
