/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.terracottatech.nomad.client.NomadEndpoint;
import com.terracottatech.nomad.client.NomadMessageSender;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public class ChangeMessageSender<T> extends NomadMessageSender<T> {
  public ChangeMessageSender(List<NomadEndpoint<T>> servers, String host, String user) {
    super(servers, host, user);
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    super.startPrepare(newChangeUuid);
    this.changeUuid = newChangeUuid;
  }

  @Override
  public void prepared(InetSocketAddress server) {
    super.prepared(server);
    registerPreparedServer(server);
  }
}
