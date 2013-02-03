package org.jvnet.hudson.plugins.port_allocator;

import java.io.IOException;

/**
 * Represents a port, that has been allocated from a pool.
 *
 * Holds a PortAllocationManager to trigger cleanup.
 *
 * @author pepov
 */
public class PooledPort extends Port {

	private int selectedPort;
	private PortAllocationManager manager;

	public PooledPort(PooledPortType portType, int selectedPort, PortAllocationManager manager) {
		super(portType);
		this.selectedPort = selectedPort;
		this.manager = manager;
	}

	@Override
	public int get() {
		return selectedPort;
	}
	
	@Override
	public void cleanUp() throws IOException, InterruptedException {
		manager.free(selectedPort);
	}
}
