package org.jvnet.hudson.plugins.port_allocator;

import java.io.IOException;

import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;

import org.jvnet.hudson.plugins.port_allocator.PortAllocationManager;
import org.jvnet.hudson.plugins.port_allocator.PortAllocationManager.PortUnavailableException;
import org.mockito.Mockito;

import junit.framework.TestCase;

public class PortAllocationManagerTest extends TestCase {

	private class TestMonitor {
		public boolean ready;
		public boolean completed;
		public Exception failure;
	}

	/**
	 * Test logical allocation of ports.
	 * This does not actually try to allocate the ports.
	 * @throws Exception
	 */
	public void testAllocate() throws Exception {
		final Computer computer = Mockito.mock(Computer.class);
		final AbstractBuild build = Mockito.mock(AbstractBuild.class);
		final PortAllocationManager manager = PortAllocationManager.getManager(computer);

		final TestMonitor monitor = new TestMonitor();

		final int testPort1 = 55;
		final int testPort2 = 56;

		Thread runner = new Thread() {

			@Override
			public void run() {
				try {
					runInner();
				} catch (IOException e) {
					synchronized (monitor) {
						monitor.failure = e;
					}
				} catch (InterruptedException e) {
					synchronized (monitor) {
						monitor.failure = e;
					}
				}
			}
			public void runInner() throws IOException, InterruptedException {
				// Initial port allocation
				int port1 = manager.allocate(build, testPort1);
				assertEquals(testPort1, port1);

				manager.free(port1);

				// Test re-allocation of port
				int port1a = manager.allocate(build, testPort1);
				assertEquals(testPort1, port1a);

				// Allocate a second port
				int port2 = manager.allocate(build, testPort2);
				assertEquals(testPort2, port2);

				manager.free(port2);

				synchronized (monitor) {
					monitor.ready = true;
					monitor.notify();
				}
				// Test double allocation of port1 - will hang until
				// master thread releases things.
				int port1b = manager.allocate(build, testPort1);
				assertEquals(testPort1, port1b);

				synchronized (monitor) {
					monitor.completed = true;
					monitor.notify();
				}
			}
		};
		runner.setDaemon(true);
		runner.start();

		synchronized (monitor) {
			for (int i = 0; i < 10 && !monitor.ready; i++) {
				monitor.wait(1000);
			}
			assertTrue("allocate thread failed to prepare", monitor.ready);
			assertFalse("allocate thread completed before port was freed", monitor.completed);
			manager.free(testPort1);
			for (int i = 0; i < 10 && !monitor.completed; i++) {
				monitor.wait(1000);
			}
			assertTrue("allocate thread failed to complete", monitor.completed);
		}
		// Give the thread a reasonable time to finish.
		runner.join(10000);
		if (monitor.failure != null) {
			// Ensure that junit sees the thread exception.
			throw monitor.failure;
		}
	}

	/**
	 * Ensure that port allocation invokes the remote callable and
	 * passes back the port allocated by the remote node.
	 * @throws Throwable
	 */
	public void testAllocateRandom() throws Throwable {
		final VirtualChannel channel = Mockito.mock(VirtualChannel.class);
		final Computer computer = Mockito.mock(Computer.class);
		final AbstractBuild build = Mockito.mock(AbstractBuild.class);

		final int mockPort = 42;
		Mockito.when(computer.getChannel()).thenReturn(channel);
		Mockito.when(channel.call(Mockito.isNotNull())).thenReturn(mockPort);

		final PortAllocationManager manager = PortAllocationManager.getManager(computer);

		int port = manager.allocateRandom(build, mockPort);
		assertEquals(mockPort, port);
		// We cannot confirm that second and subsequent allocations of port 42
		// allocation will succeed because mocking allocatePort and/or Callable is
		// difficult.

		// Ensure that free port continues without problems.
		manager.free(port);
	}

	/**
	 * Ensure that port allocation invokes the remote callable and
	 * passes back the port allocated by the remote node.
	 * @throws Throwable
	 */
	public void testAllocatePortRange() throws Throwable {
		final VirtualChannel channel = Mockito.mock(VirtualChannel.class);
		final Computer computer = Mockito.mock(Computer.class);
		final AbstractBuild build = Mockito.mock(AbstractBuild.class);

		final int mockStart = 42;
		final int mockEnd = 48;
		final int mockPort = 44;
		Mockito.when(computer.getChannel()).thenReturn(channel);

		Mockito.when(channel.call(Mockito.isNotNull()))
								// First port succeeds
								.thenReturn(mockPort)
								// Second port fails
								.thenThrow(PortUnavailableException.class)
								// Better luck next time around
								.thenReturn(mockPort + 2)
								.thenReturn(mockPort + 3);

		final PortAllocationManager manager = PortAllocationManager.getManager(computer);

		int[] ports = manager.allocatePortRange(build, mockStart, mockEnd, 2, true);
		assertNotNull(ports);
		assertEquals(2, ports.length);
		assertEquals(mockPort + 2, ports[0]);
		assertEquals(mockPort + 3, ports[1]);
		// We cannot confirm that second and subsequent allocations of port 42
		// allocation will succeed because mocking allocatePort and/or Callable is
		// difficult.

		// Ensure that free port continues without problems.
		manager.free(ports[0]);
		manager.free(ports[1]);
	}
}
