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
package org.terracotta.diagnostic.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Version;
import org.terracotta.connection.Connection;
import org.terracotta.connection.Diagnostics;
import org.terracotta.diagnostic.common.Base64DiagnosticCodec;
import org.terracotta.diagnostic.common.DiagnosticCodec;
import org.terracotta.diagnostic.common.DiagnosticRequest;
import org.terracotta.diagnostic.common.DiagnosticResponse;
import org.terracotta.diagnostic.common.EmptyParameterDiagnosticCodec;
import org.terracotta.diagnostic.model.KitInformation;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.exception.ConnectionClosedException;

import java.lang.reflect.Proxy;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_DIAGNOSTIC_EXTENSIONS;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_DIAGNOSTIC_REQUEST_HANDLER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_SERVER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_INVALID_JMX;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_NOT_PERMITTED;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_NULL_RETURN;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_REQUEST_TIMEOUT;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_UNKNOWN_COMMAND;

/**
 * @author Mathieu Carbou
 */
class DiagnosticServiceImpl implements DiagnosticService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticServiceImpl.class);

  private final Connection connection;
  private final Diagnostics delegate;
  private final DiagnosticCodec<String> codec;

  DiagnosticServiceImpl(Connection connection, Diagnostics delegate, DiagnosticCodec<?> codec) {
    this.connection = requireNonNull(connection);
    this.delegate = requireNonNull(delegate);
    // we need to ensure the JMX parameter contains no space at all because the DiagnosticsHandler is poorly written,
    // and it is parsing parameters by splitting the string using space character
    this.codec = new EmptyParameterDiagnosticCodec()
        .around(new Base64DiagnosticCodec())
        .around(codec);
  }

  // Diagnostics

  @Override
  public boolean isConnected() {
    try {
      return connection.isValid();
    } catch (ConnectionClosedException e) {
      return false;
    }
  }

  @Override
  public <T> T getProxy(Class<T> type) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException, DiagnosticOperationUnsupportedException {
    requireNonNull(type);
    if (!type.isInterface()) {
      throw new IllegalArgumentException("Interface expected");
    }
    if (!Boolean.parseBoolean(invokeWithArg(MBEAN_DIAGNOSTIC_REQUEST_HANDLER, "hasServiceInterface", type.getName()))) {
      throw new DiagnosticOperationUnsupportedException(type.getName());
    }
    return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
      if (method.getDeclaringClass() == Object.class) {
        switch (method.getName()) {
          case "equals":
            return proxy == args[0];
          case "hashCode":
            return type.hashCode();
          case "toString":
            return format("DiagnosticServiceProxy(%s)", type.getName());
          default:
            break;
        }
      }
      return invokeEncoded(type, method.getName(), method.getReturnType(), args);
    }));
  }

  @Override
  public void close() {
    try {
      delegate.close();
    } catch (Exception e) {
      LOGGER.debug("Failed to close diagnostic entity", e);
    }

    try {
      connection.close();
    } catch (Exception e) {
      LOGGER.debug("Failed to close diagnostic connection", e);
    }
  }

  // DiagnosticMBeanSupport

  @Override
  public String get(String name, String attribute) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return execute(() -> delegate.get(name, attribute));
  }

  @Override
  public void set(String name, String attribute, String arg) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    execute(() -> delegate.set(name, attribute, arg));
  }

  @Override
  public String invoke(String name, String cmd) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return execute(() -> delegate.invoke(name, cmd));
  }

  @Override
  public String invokeWithArg(String name, String cmd, String arg) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return execute(() -> delegate.invokeWithArg(name, cmd, arg));
  }

  // LogicalServerState

  @Override
  public LogicalServerState getLogicalServerState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    try {
      return LogicalServerState.parse(invoke(MBEAN_DIAGNOSTIC_EXTENSIONS, "getLogicalServerState"));
    } catch (DiagnosticOperationUnsupportedException | DiagnosticOperationExecutionException ignored) {
      // maybe we connect to an old version, 10.2 for example, that does not have this MBean. In this case, let's try the original Server state Mbean.
      // Other possibility: the MBean has been unregistered...
    }

    // backward compatibility
    try {
      return LogicalServerState.parse(invoke("LogicalServerState", "getLogicalServerState"));
    } catch (DiagnosticOperationUnsupportedException | DiagnosticOperationExecutionException ignored) {
    }

    String state = LogicalServerState.UNKNOWN.name();
    try {
      state = getState();
    } catch (DiagnosticOperationUnsupportedException | DiagnosticOperationExecutionException ignored) {
      // should never occur for getState()
    }

    boolean blocked = false;
    try {
      blocked = isBlocked();
    } catch (DiagnosticOperationUnsupportedException | DiagnosticOperationExecutionException e2) {
      // in case ConsistencyManager is not there
    }

    return LogicalServerState.from(state, isReconnectWindow(), blocked);
  }

  @Override
  public KitInformation getKitInformation() throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    try {
      // Implemented after 10.7.0.3
      String props = invoke(MBEAN_DIAGNOSTIC_EXTENSIONS, "getKitInformation");
      return KitInformation.fromProperties(props);

    } catch (RuntimeException e) {
      // Backward compatible with 10.7.0.3 and below

      String v = invoke(MBEAN_SERVER, "getVersion"); // something like "Terracotta 5.8.2-pre6"
      String b = invoke(MBEAN_SERVER, "getBuildID"); // something like "2021-06-29 at 20:54:46 UTC (Revision 4450fe6fc2c174abd3528b8636b3296a6a79df00 from UNKNOWN)"

      int pos = v.indexOf(' ');
      String version = pos == -1 ? v : v.substring(pos + 1); // the moniker is hard-coded in core project and can be Terracotta or terracotta-enterprise

      Matcher matcher = Pattern.compile("^(?<date>.*) \\(Revision (?<revision>.*) from (?<branch>.*)\\)$").matcher(b);
      if (matcher.matches()) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH:mm:ss z"); // from core
        Instant timestamp = dtf.parse(matcher.group("date"), Instant::from);
        return new KitInformation(Version.valueOf(version), matcher.group("revision"), matcher.group("branch"), timestamp);
      } else {
        return new KitInformation(Version.valueOf(version), "UNKNOWN", "UNKNOWN", Instant.ofEpochMilli(0L));
      }
    }
  }

  // DiagnosticsHandler

  @Override
  public String getInitialState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    return execute(delegate::getInitialState);
  }

  @Override
  public String getClusterState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    return execute(delegate::getClusterState);
  }

  @Override
  public String getConfig() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    return execute(delegate::getConfig);
  }

  @Override
  public String getProcessArguments() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    return execute(delegate::getProcessArguments);
  }

  @Override
  public String getThreadDump() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    return execute(delegate::getThreadDump);
  }

  @Override
  public void terminateServer() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    execute(true, delegate::terminateServer);
  }

  @Override
  public void forceTerminateServer() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    execute(true, delegate::forceTerminateServer);
  }

  private String getState() throws DiagnosticOperationTimeoutException, DiagnosticConnectionException {
    return execute(delegate::getState);
  }

  @SuppressWarnings("unchecked")
  private <T> T invokeEncoded(Class<?> serviceType, String methodName, Class<T> returnType, Object... args) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException, DiagnosticOperationUnsupportedException {
    LOGGER.trace("invokeEncoded({}, {})", serviceType.getName(), methodName);
    DiagnosticRequest request = new DiagnosticRequest(serviceType, methodName, args);
    String encodedReq = codec.serialize(request);
    String encodedRes = invokeWithArg(MBEAN_DIAGNOSTIC_REQUEST_HANDLER, "request", encodedReq);
    DiagnosticResponse<T> response = codec.deserialize(encodedRes, DiagnosticResponse.class);
    response.getError().map(DiagnosticOperationExecutionException::new).ifPresent(e -> {
      throw e;
    });
    return returnType.isPrimitive() ?
        response.getBody() :
        returnType.cast(returnType == Optional.class ?
            Optional.ofNullable(response.getBody()) :
            response.getBody());
  }

  private String execute(Supplier<String> execution) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    return execute(false, execution);
  }

  private String execute(boolean allowNull, Supplier<String> execution) throws DiagnosticOperationTimeoutException, DiagnosticOperationExecutionException, DiagnosticConnectionException {
    // JMXSubsystem transforms all null returns to empty strings.
    // null returns are in the case of EntityException, InterruptedException or MessageCodecException,
    // or in the case of terminateServer or forceTerminateServer
    try {
      String result = execution.get();
      if (result == null) {
        if (allowNull) {
          return null;
        } else {
          // a failure happened (EntityException, InterruptedException or MessageCodecException)
          throw new DiagnosticConnectionException();
        }
      }
      if (MESSAGE_NULL_RETURN.equals(result)) {
        // convert back to null empty strings
        return null;
      }
      // Handles all the errors from JMXSubsystem and DiagnosticsHandler
      if (MESSAGE_NOT_PERMITTED.equals(result)) {
        throw new DiagnosticOperationNotAllowedException(result);
      }
      if (MESSAGE_REQUEST_TIMEOUT.equals(result)) {
        throw new DiagnosticOperationTimeoutException(result);
      }
      if (MESSAGE_UNKNOWN_COMMAND.equals(result)) {
        throw new DiagnosticOperationUnsupportedException(result);
      }
      if (result.startsWith(MESSAGE_INVALID_JMX)) {
        throw new DiagnosticOperationExecutionException(result);
      }
      return result;
    } catch (ConnectionClosedException e) {
      throw new DiagnosticConnectionException(e);
    }
  }

}
