/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracotta.connection.api.TerracottaConnectionService;
import com.terracottatech.diagnostic.client.DiagnosticOperationExecutionException;
import com.terracottatech.dynamic_config.cli.ConfigTool;
import com.terracottatech.tools.client.TopologyEntity;
import com.terracottatech.tools.client.TopologyEntityProvider;
import com.terracottatech.tools.config.ClusterConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityException;

import java.net.URI;
import java.util.Properties;

import static java.io.File.separator;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class SimpleSetCommandIT extends BaseStartupIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    forEachNode((stripeId, nodeId, port) -> startNode(
        "--node-name", "node-" + nodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(port),
        "--node-group-port", String.valueOf(port + 10),
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
        "--node-backup-dir", "backup/stripe" + stripeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId));

    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));
  }

  /*<--Single Node Tests-->*/
  @Test
  public void setOffheapResource() {
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main=512MB");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main");
    waitedAssert(out::getLog, containsString("offheap-resources.main=512MB"));
  }

  @Test
  public void setOffheapResource_postActivation_decreaseSize() throws Exception {
    activateCluster();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("should be larger than the old size"));
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main=1MB");
  }

  @Test
  public void setOffheapResource_postActivation_licenseViolation() throws Exception {
    activateCluster();

    exception.expect(DiagnosticOperationExecutionException.class);
    exception.expectMessage(containsString("not within the license limits"));
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main=10TB");
  }

  @Test
  public void setOffheapResource_postActivation_increaseSize() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main=1GB");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main");
    waitedAssert(out::getLog, containsString("offheap-resources.main=1GB"));
  }

  @Test
  public void setOffheapResource_postActivation_addResource() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources=second:1GB");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.second");
    waitedAssert(out::getLog, containsString("offheap-resources.second=1GB"));
  }

  @Test
  public void setOffheapResources_postActivation_addResources() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.third=1GB");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.second", "-c", "offheap-resources.third");
    waitedAssert(out::getLog, containsString("offheap-resources.second=1GB"));
    waitedAssert(out::getLog, containsString("offheap-resources.third=1GB"));
  }

  @Test
  public void setOffheapResources_postActivation_addResource_increaseSize() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources=main:1GB,second:1GB");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources");
    waitedAssert(out::getLog, containsString("offheap-resources=main:1GB,second:1GB"));
  }

  @Test
  public void setOffheapResources_postActivation_newResource_decreaseSize() throws Exception {
    activateCluster();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("should be larger than the old size"));
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.main=1MB");
  }

  @Test
  public void setTcProperties() {
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.something=value");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.something");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.tc-properties.something=value"));
  }

  @Test
  public void setClientReconnectWindow() {
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "client-reconnect-window=10s");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "client-reconnect-window");
    waitedAssert(out::getLog, containsString("client-reconnect-window=10s"));
  }

  @Test
  public void setSecurityAuthc() {
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "security-dir=/path/to/security/dir", "-c", "security-authc=file");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "security-authc");
    waitedAssert(out::getLog, containsString("security-authc=file"));
  }

  @Test
  public void setNodeGroupPort() {
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.node-group-port=9630");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.node-group-port");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-group-port=9630"));
  }

  @Test
  public void setSecurityWhitelist() {
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "security-dir=/path/to/security/dir", "-c", "security-whitelist=true");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "security-whitelist");
    waitedAssert(out::getLog, containsString("security-whitelist=true"));
  }

  @Test
  public void setDataDir() {
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.data-dirs.main=user-data/main/stripe1-node1-data-dir");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.data-dirs.main");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs.main=user-data" + separator + "main" + separator + "stripe1-node1-data-dir"));
  }

  @Test
  public void setDataDir_postActivation_updatePath() throws Exception {
    activateCluster();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("A data directory with name: main already exists"));
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs.main=user-data/main/stripe1-node1-data-dir");
  }

  @Test
  public void setDataDir_postActivation_overlappingPaths() throws Exception {
    activateCluster();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("overlaps with the existing data directory"));
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs.first=user-data/main/stripe1/node1");
  }

  @Test
  public void setDataDir_postActivation_addMultipleNonExistentDataDirs_overLappingPaths() throws Exception {
    activateCluster();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("overlaps with the existing data directory"));
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-1");
  }

  @Test
  public void setDataDir_postActivation_addMultipleNonExistentDataDirs_overLappingPaths_flavor2() throws Exception {
    activateCluster();

    exception.expect(IllegalStateException.class);
    exception.expectMessage(containsString("overlaps with the existing data directory"));
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-1");
  }

  @Test
  public void setDataDir_postActivation_addOneNonExistentDataDir() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs.second");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));
  }

  @Test
  public void setDataDir_postActivation_addMultipleNonExistentDataDirs() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-2");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs.second");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));

    out.clearLog();
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs.third");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2"));
  }

  @Test
  public void setDataDir_postActivation_addMultipleNonExistentDataDirs_flavor2() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-2");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs.second");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));

    out.clearLog();
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "data-dirs.third");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2"));
  }

  @Test
  public void setNodeBackupDir() {
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.node-backup-dir=backup/stripe1-node1-backup");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.node-backup-dir");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-backup-dir=backup" + separator + "stripe1-node1-backup"));
  }

  @Test
  public void setTwoProperties() {
    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main=1GB", "-c", "stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir");
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "offheap-resources.main", "-c", "stripe.1.node.1.data-dirs.main");
    waitedAssert(out::getLog, containsString("offheap-resources.main=1GB"));
    waitedAssert(out::getLog, containsString("stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir"));
  }

  @Test
  public void setFailover_Priority_postActivation_Consistency() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "failover-priority=consistency:2");
    waitedAssert(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "failover-priority");
    waitedAssert(out::getLog, containsString("failover-priority=consistency:2"));
  }

  @Test
  public void setNodeLogDir_postActivation() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "node-log-dir=logs/stripe1");
    waitedAssert(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "node-log-dir");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-log-dir=logs" + separator + "stripe1"));
  }

  @Test
  public void setNodeBindAddress_postActivation() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "node-bind-address=127.0.0.1");
    waitedAssert(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "node-bind-address");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-bind-address=127.0.0.1"));
  }

  @Test
  public void setNodeGroupBindAddress_postActivation() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "node-group-bind-address=127.0.0.1");
    waitedAssert(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "node-group-bind-address");
    waitedAssert(out::getLog, containsString("stripe.1.node.1.node-group-bind-address=127.0.0.1"));
  }

  @Test
  public void testTcProperty_postActivation() throws Exception {
    activateCluster();

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "tc-properties.foo=bar");
    waitedAssert(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-r", "-s", "localhost:" + ports.getPort(), "-c", "tc-properties");
    waitedAssert(out::getLog, not(containsString("tc-properties=foo:bar")));

    out.clearLog();
    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "tc-properties");
    waitedAssert(out::getLog, containsString("tc-properties=foo:bar"));

    out.clearLog();
    ConfigTool.start("unset", "-s", "localhost:" + ports.getPort(), "-c", "tc-properties.foo");
    waitedAssert(out::getLog, containsString("restart of the cluster is required"));
    assertCommandSuccessful();

    ConfigTool.start("get", "-s", "localhost:" + ports.getPort(), "-c", "tc-properties");
    waitedAssert(out::getLog, not(containsString("tc-properties=foo:bar")));
  }

  @Test
  public void testPublicHostPort() throws Exception {
    activateCluster();

    try (Connection connection = new TerracottaConnectionService().connect(URI.create("terracotta://localhost:" + ports.getPort()), new Properties())) {
      ClusterConfiguration configuration = getTopologyEntity(connection).getClusterConfiguration();
      assertThat(configuration.getHostPortsMap().size(), is(equalTo(1)));
      assertThat(configuration.getHostPortsMap(), hasEntry("localhost:" + ports.getPort(), "localhost:" + ports.getPort()));
    }

    ConfigTool.start("set", "-s", "localhost:" + ports.getPort(),
        "-c", "stripe.1.node.1.node-public-hostname=127.0.0.1",
        "-c", "stripe.1.node.1.node-public-port=" + ports.getPort());
    assertCommandSuccessful();

    try (Connection connection = new TerracottaConnectionService().connect(URI.create("terracotta://localhost:" + ports.getPort()), new Properties())) {
      ClusterConfiguration configuration = getTopologyEntity(connection).getClusterConfiguration();
      assertThat(configuration.getHostPortsMap().size(), is(equalTo(1)));
      assertThat(configuration.getHostPortsMap(), hasEntry("localhost:" + ports.getPort(), "127.0.0.1:" + ports.getPort()));
    }
  }

  private static TopologyEntity getTopologyEntity(Connection base) throws ConnectionException {
    try {
      EntityRef<TopologyEntity, Void, Void> ref = base.getEntityRef(TopologyEntity.class, TopologyEntityProvider.ENTITY_VERSION, TopologyEntityProvider.ENTITY_NAME);
      return ref.fetchEntity(null);
    } catch (EntityException ee) {
      throw new ConnectionException(ee);
    }
  }
}
