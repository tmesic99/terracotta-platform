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
package org.terracotta.management.model.cluster;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ClusterTest extends AbstractTest {

  @Test
  public void keep_tags_ordering() {
    Client client = Client.create("12345@127.0.0.1:ehcache:uid")
        .addTags("webapp-1", "server-1", "cluster-1");
    List<String> tags = new ArrayList<>(client.getTags());
    assertThat(tags.size(), equalTo(3));
    assertThat(tags.get(0), equalTo("webapp-1"));
    assertThat(tags.get(1), equalTo("server-1"));
    assertThat(tags.get(2), equalTo("cluster-1"));
  }

  @Test
  public void test_serialization() throws IOException, ClassNotFoundException {
    assertEquals(cluster1, cluster2);

    // ensure parent ref is the same ref as another node within the topology
    assertSame(cluster1.getStripe("stripe-1").get(), cluster1.getStripe("stripe-1").get().getServerByName("server-1").get().getStripe());
    assertSame(cluster1, cluster1.getStripe("stripe-1").get().getServerByName("server-1").get().getStripe().getCluster());

    Cluster c1_copy = copy(cluster1);
    Cluster c2_copy = copy(cluster2);
    assertEquals(cluster1, c1_copy);
    assertEquals(cluster1, c2_copy);

    // ensure parent ref is the same ref as another node within the topology
    assertSame(c1_copy.getStripe("stripe-1").get(), c1_copy.getStripe("stripe-1").get().getServerByName("server-1").get().getStripe());
    assertSame(c1_copy, c1_copy.getStripe("stripe-1").get().getServerByName("server-1").get().getStripe().getCluster());
  }

  @Test
  public void test_equals_hashcode() {
    assertEquals(cluster2, cluster1);
    assertEquals(cluster2.hashCode(), cluster1.hashCode());
  }

  @Test
  public void test_nodes_path() throws UnknownHostException {
    assertEquals(3, cluster1.getActiveServerEntity(ehcache_server_entity.getContext()).get().getNodePath().size());
    assertEquals(
        "stripe-1/server-1/ehcache-entity-name-1:org.ehcache.clustered.client.internal.EhcacheClientEntity",
        cluster1.getActiveServerEntity(ehcache_server_entity.getContext()).get().getStringPath());

    assertEquals(6, ehcache_server_entity.getContext().size());

    assertEquals(3, cluster1.getNodes(ehcache_server_entity.getContext()).size());
    assertEquals("[stripe-1, server-1, ehcache-entity-name-1:org.ehcache.clustered.client.internal.EhcacheClientEntity]", cluster1.getNodes(ehcache_server_entity.getContext()).toString());

    assertEquals(1, cluster1.getNodes(client.getContext()).size());
    assertEquals(
        "[12345@127.0.0.1:ehcache:uid]",
        cluster1.getNodes(client.getContext()).toString());
  }

  @Test
  public void test_add_remove_client() {
    assertEquals(1, cluster1.getClients().size());

    assertFalse(cluster1.addClient(Client.create("12345@127.0.0.1:ehcache:uid")));

    assertEquals(1, cluster1.getClients().size());

    assertTrue(cluster1.addClient(Client.create("123@127.0.0.1:cluster-client-2:uid")));

    assertEquals(2, cluster1.getClients().size());

    assertTrue(cluster1.removeClient("123@127.0.0.1:cluster-client-2:uid").isPresent());
    assertFalse(cluster1.getClient("123@127.0.0.1:cluster-client-2:uid").isPresent());
    assertEquals(1, cluster1.getClients().size());
  }

  @Test
  public void test_add_remove_stripes() {
    assertEquals(2, cluster1.getStripes().size());

    assertFalse(cluster1.addStripe(Stripe.create("stripe-1")));

    assertEquals(2, cluster1.getStripes().size());

    cluster1.addStripe(Stripe.create("stripe-3"));

    assertEquals(3, cluster1.getStripes().size());

    assertTrue(cluster1.removeStripe("stripe-3").isPresent());
    assertFalse(cluster1.getStripe("stripe-3").isPresent());
    assertEquals(2, cluster1.getStripes().size());
  }

  @Test
  public void test_add_remove_server() {
    Stripe stripe = cluster1.getStripe("stripe-1").get();

    assertEquals(2, stripe.getServers().size());

    assertFalse(stripe.addServer(Server.create("server-1")));

    assertEquals(2, stripe.getServers().size());

    stripe.addServer(Server.create("server-3"));

    assertEquals(3, stripe.getServers().size());

    assertTrue(stripe.removeServerByName("server-3").isPresent());
    assertFalse(stripe.getServerByName("server-3").isPresent());
    assertEquals(2, stripe.getServers().size());
  }

  @Test
  public void test_add_remove_connection() {
    Client client = cluster1.getClient("12345@127.0.0.1:ehcache:uid").get();

    assertEquals(2, client.getConnections().size());

    assertFalse(client.addConnection(Connection.create("uid", cluster1.getStripe("stripe-1").get().getServerByName("server-1").get(), Endpoint.create("10.10.10.10", 3456))));
    assertEquals(2, client.getConnections().size());

    client.addConnection(Connection.create("uid", cluster1.getStripe("stripe-1").get().getServerByName("server-1").get(), Endpoint.create("10.10.10.10", 3458)));

    assertEquals(3, client.getConnections().size());

    assertTrue(client.removeConnection("uid:stripe-1:server-1:10.10.10.10:3458").isPresent());
    assertFalse(client.getConnection("uid:stripe-1:server-1:10.10.10.10:3458").isPresent());
    assertEquals(2, client.getConnections().size());
  }

  @Test
  public void test_add_remove_server_entity() {
    Server server = cluster1.stripeStream().findAny().get().getActiveServer().get();

    assertEquals(1, server.getServerEntityCount());

    assertFalse(server.addServerEntity(ServerEntity.create(serverContextContainer.getValue(), "org.ehcache.clustered.client.internal.EhcacheClientEntity")));

    assertEquals(1, server.getServerEntityCount());

    assertTrue(server.addServerEntity(ServerEntity.create("other-cm-4", "org.ehcache.clustered.client.internal.EhcacheClientEntity")));
    assertTrue(server.addServerEntity(ServerEntity.create("name", "OTHER_TYPE")));

    assertEquals(3, server.getServerEntityCount());

    assertTrue(server.removeServerEntity("other-cm-4:" + "org.ehcache.clustered.client.internal.EhcacheClientEntity").isPresent());
    assertFalse(server.getServerEntity("other-cm-4:" + "org.ehcache.clustered.client.internal.EhcacheClientEntity").isPresent());
    assertEquals(2, server.getServerEntityCount());
  }

  @Test
  public void test_fetch_unfetch() throws IOException {
    Connection connection = client.connectionStream().findAny().get();
    assertFalse(connection.hasFetchedServerEntity("ehcache-entity-name-1", "org.ehcache.clustered.client.internal.EhcacheClientEntity"));

    assertTrue(connection.fetchServerEntity("ehcache-entity-name-1", "org.ehcache.clustered.client.internal.EhcacheClientEntity"));
    assertTrue(connection.hasFetchedServerEntity("ehcache-entity-name-1", "org.ehcache.clustered.client.internal.EhcacheClientEntity"));

    assertTrue(connection.unfetchServerEntity("ehcache-entity-name-1", "org.ehcache.clustered.client.internal.EhcacheClientEntity"));
    assertFalse(connection.hasFetchedServerEntity("ehcache-entity-name-1", "org.ehcache.clustered.client.internal.EhcacheClientEntity"));

    // can fetch several times the same entity
    assertTrue(connection.fetchServerEntity("ehcache-entity-name-1", "org.ehcache.clustered.client.internal.EhcacheClientEntity"));
    assertTrue(connection.fetchServerEntity("ehcache-entity-name-1", "org.ehcache.clustered.client.internal.EhcacheClientEntity"));
    assertTrue(connection.unfetchServerEntity("ehcache-entity-name-1", "org.ehcache.clustered.client.internal.EhcacheClientEntity"));
    assertTrue(connection.unfetchServerEntity("ehcache-entity-name-1", "org.ehcache.clustered.client.internal.EhcacheClientEntity"));
    assertFalse(connection.unfetchServerEntity("ehcache-entity-name-1", "org.ehcache.clustered.client.internal.EhcacheClientEntity"));
  }

  @Test
  public void test_toMap() {
    final Json json = new DefaultJsonFactory().create();

    Map<String, Object> actual = json.mapToObject(cluster1.toMap());
    Map<String, Object> expected = json.parseObject(getClass().getResource("/cluster.json"));

    assertEquals(expected, actual);
  }

  @SuppressWarnings("unchecked")
  private static <T> T copy(T o) throws IOException, ClassNotFoundException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    oos.writeObject(o);
    oos.close();
    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
    return (T) in.readObject();
  }
}
