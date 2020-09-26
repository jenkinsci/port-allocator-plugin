package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.AbstractBuild;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ClusterPortAllocationManager implements PortAllocationManager {

  /**
   * Ports currently in use, to the build that uses it.
   */
  private final Set<Integer> ports = new HashSet<Integer>();

  public int allocateRandom(AbstractBuild owner, int prefPort) throws InterruptedException, IOException {
    throw new UnsupportedOperationException("Allocate a random port is not available in cluster mode.");
  }

  public synchronized int allocate(AbstractBuild owner, int port) throws InterruptedException, IOException {
    while (ports.contains(port)) {
      wait();
    }

    ports.add(port);

    return port;
  }

  public synchronized boolean isFree(int port) {
    return !ports.contains(port);
  }

  public synchronized void free(int n) {
    ports.remove(n);
    notifyAll(); // wake up anyone who's waiting for this port
  }

}
