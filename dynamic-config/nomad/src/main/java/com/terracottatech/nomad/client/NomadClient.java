/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.client.change.ChangeProcess;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.recovery.RecoveryProcess;
import com.terracottatech.nomad.client.recovery.RecoveryResultReceiver;

import java.util.List;

public class NomadClient<T> {
  private final List<NomadEndpoint<T>> servers;
  private final String host;
  private final String user;

  /**
   * @param servers the set of servers to run the Nomad protocol across
   * @param host    the name of the local machine
   * @param user    the name of the user the current process is running as
   */
  public NomadClient(List<NomadEndpoint<T>> servers, String host, String user) {
    if (servers.isEmpty()) {
      throw new IllegalArgumentException("There must be at least one server");
    }

    this.servers = servers;
    this.host = host;
    this.user = user;
  }

  public void tryApplyChange(ChangeResultReceiver<T> results, NomadChange change) {
    ChangeProcess<T> changeProcess = new ChangeProcess<>(servers, host, user);
    changeProcess.applyChange(results, change);
  }

  public void tryRecovery(RecoveryResultReceiver<T> results) {
    RecoveryProcess<T> recoveryProcess = new RecoveryProcess<>(servers, host, user);
    recoveryProcess.recover(results);
  }
}
