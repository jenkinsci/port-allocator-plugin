package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.Computer;
import hudson.remoting.Callable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;


/**
 * Manages ports in use.
 *
 * @author Rama Pulavarthi
 */
final class PortAllocationManager {
    private final Computer node;


    /**
     * Ports currently in use.
     */
    private final Set<Integer> ports = new HashSet<Integer>();

    private static final Map<Computer, WeakReference<PortAllocationManager>> INSTANCES =
            new WeakHashMap<Computer, WeakReference<PortAllocationManager>>();

    private PortAllocationManager(Computer node) {
        this.node = node;
    }

    /**
     * Allocates a port on the Computer where the jobs gets executed.
     *
     * @param port
     * @param prefPort Preffered Port
     * @return if port =0
     *         tries to assign the preffered port, if the preffered
     *         port is not available assigns a random available port
     *         else
     *         assigns the requested port if free or waits on it
     */
    public synchronized int allocate(int port, int prefPort) {
        try {
            int i;
            if (port == 0) {
                try {
                    // try to allocate preferential Port,
                    i = allocatePort(prefPort);
                } catch (IOException ex) {
                    // if not available assign a free port
                    i = allocatePort(0);
                }
            } else {
                try {
                    i = allocatePort(port);
                } catch (IOException ex) {
                    //TODO Requested Port is not available, wait for it
                    // change this, for now assign a  free port
                    i = allocatePort(0);
                }
            }
            ports.add(i);
            return i;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static PortAllocationManager getManager(Computer node) {
        PortAllocationManager pam;
        WeakReference<PortAllocationManager> ref = INSTANCES.get(node);
        if (ref != null) {
            pam = ref.get();
            if (pam != null)
                return pam;
        }
        pam = new PortAllocationManager(node);
        INSTANCES.put(node, new WeakReference<PortAllocationManager>(pam));
        return pam;
    }

    public synchronized void free(int n) {
        //Check if port is still in use
        try {
            if (!isPortInUse(n)) {
                ports.remove(n);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    private boolean isPortInUse(final int port) throws InterruptedException {
        try {
            return node.getChannel().call(new Callable<Boolean, IOException>() {
                public Boolean call() throws IOException {
                    new ServerSocket(port).close();
                    return true;
                }
            });
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * @param port 0 to assign a free port
     * @return port that gets assigned
     * @throws InterruptedException
     * @throws IOException
     */
    private int allocatePort(final int port) throws InterruptedException, IOException {
        return node.getChannel().call(new Callable<Integer, IOException>() {
            public Integer call() throws IOException {
                ServerSocket server = new ServerSocket(port);
                int localPort = server.getLocalPort();
                server.close();
                return localPort;
            }
        });
    }
}
