/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import com.terracottatech.diagnostic.client.connection.DiagnosticServices;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.TimeUnitConverter;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.ClusterFactory;
import com.terracottatech.dynamic_config.model.ClusterValidator;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.TimeUnit;
import com.terracottatech.utilities.Tuple2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;

import static com.terracottatech.utilities.Assertions.assertNonNull;
import static java.lang.System.lineSeparator;

@Parameters(commandNames = "activate", commandDescription = "Activate the cluster")
@Usage("activate <-s HOST[:PORT] | -f CONFIG-PROPERTIES-FILE> -n CLUSTER-NAME -l LICENSE-FILE")
public class ActivateCommand extends RemoteCommand {

  @Parameter(names = {"-s"}, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "Config properties file", converter = PathConverter.class)
  private Path configPropertiesFile;

  @Parameter(names = {"-n"}, description = "Cluster name")
  private String clusterName;

  @Parameter(required = true, names = {"-l"}, description = "Path to license file", converter = PathConverter.class)
  private Path licenseFile;

  @Parameter(names = {"-rwt", "--restart-wait-time"}, description = "Restart wait time", converter = TimeUnitConverter.class)
  private Measure<TimeUnit> restartWaitTime = Measure.of(1, TimeUnit.MINUTES);

  private Cluster cluster;
  private Collection<InetSocketAddress> runtimePeers;

  @Override
  public void validate() {
    if (node == null && configPropertiesFile == null) {
      throw new IllegalArgumentException("One of node or config properties file must be specified");
    }

    if (node != null && configPropertiesFile != null) {
      throw new IllegalArgumentException("Either node or config properties file should be specified, not both");
    }

    if (node != null && clusterName == null) {
      throw new IllegalArgumentException("Cluster name should be provided when node is specified");
    }

    if (node != null) {
      validateAddress(node);
    }

    assertNonNull(licenseFile, "licenseFile must not be null");
    assertNonNull(multiDiagnosticServiceProvider, "multiDiagnosticServiceProvider must not be null");
    assertNonNull(nomadManager, "nomadManager must not be null");
    assertNonNull(restartService, "restartService must not be null");

    if (!Files.exists(licenseFile)) {
      throw new ParameterException("License file not found: " + licenseFile);
    }

    cluster = loadCluster();
    runtimePeers = node == null ? cluster.getNodeAddresses() : findRuntimePeers(node);

    // check if we want to override the cluster name
    if (clusterName != null) {
      cluster.setName(clusterName);
    }

    if (cluster.getName() == null) {
      throw new IllegalArgumentException("Cluster name is missing");
    }

    // validate the topology
    new ClusterValidator(cluster).validate();

    // verify the activated state of the nodes
    boolean isClusterActive = areAllNodesActivated(runtimePeers);
    if (isClusterActive) {
      throw new IllegalStateException("Cluster is already activated");
    }
  }

  @Override
  public final void run() {
    logger.info("Activating cluster: {} formed with nodes: {}", cluster.getName(), toString(runtimePeers));

    try (DiagnosticServices diagnosticServices = multiDiagnosticServiceProvider.fetchOnlineDiagnosticServices(runtimePeers)) {
      dynamicConfigServices(diagnosticServices)
          .map(Tuple2::getT2)
          .forEach(service -> service.prepareActivation(cluster, read(licenseFile)));
      logger.info("License installation successful");
    }

    runNomadChange(new ArrayList<>(runtimePeers), new ClusterActivationNomadChange(cluster));
    logger.debug("Configuration repositories have been created for all nodes");

    logger.info("Restarting nodes: {}", toString(runtimePeers));
    restartNodes(runtimePeers, Duration.ofMillis(restartWaitTime.getQuantity(TimeUnit.MILLISECONDS)));
    logger.info("All nodes came back up");

    logger.info("Command successful!" + lineSeparator());
  }

  /*<-- Test methods --> */
  Cluster getCluster() {
    return cluster;
  }

  ActivateCommand setNode(InetSocketAddress node) {
    this.node = node;
    return this;
  }

  ActivateCommand setConfigPropertiesFile(Path configPropertiesFile) {
    this.configPropertiesFile = configPropertiesFile;
    return this;
  }

  ActivateCommand setClusterName(String clusterName) {
    this.clusterName = clusterName;
    return this;
  }

  ActivateCommand setLicenseFile(Path licenseFile) {
    this.licenseFile = licenseFile;
    return this;
  }

  private Cluster loadCluster() {
    Cluster cluster;
    if (node != null) {
      cluster = getUpcomingCluster(node);
      logger.debug("Cluster topology validation successful");
    } else {
      ClusterFactory clusterCreator = new ClusterFactory();
      cluster = clusterCreator.create(configPropertiesFile);
      logger.debug("Config property file parsed and cluster topology validation successful");
    }
    return cluster;
  }

  private static String read(Path path) {
    try {
      return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
