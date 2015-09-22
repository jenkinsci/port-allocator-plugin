package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.AbstractBuild;

import java.io.IOException;

public interface PortAllocationManager {

  /**
   * Assigns the requested port.
   *
   * This method blocks until the port becomes available.
   */
  int allocate(AbstractBuild owner, int port) throws InterruptedException, IOException;

  /**
   * Allocates a random port on the Computer where the jobs gets executed.
   *
   * <p>
   * If the preferred port is not available, assigns a random available port.
   *
   * @param prefPort
   *      Preffered port. This method trys to assign this port, and upon failing, fall back to
   *      assigning a random port.
   */
  int allocateRandom(AbstractBuild owner, int prefPort) throws InterruptedException, IOException;

  void free(int n);

  boolean isFree(int port);

}
