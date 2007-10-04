package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.Computer;
import hudson.remoting.Callable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
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

    public synchronized int allocate() {
        try {
            int i = findFreePort();
            ports.add(i);
            return i;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void free(int n) {
        //Check if port is still in use
        if(!isPortInUse(n)) {
            ports.remove(n);
        }
    }

    private static int findFreePort()
            throws IOException {
        ServerSocket server = new ServerSocket(0);
        int port = server.getLocalPort();
        server.close();
        return port;
    }

    private synchronized int allocateInRange(int lower, int higher) {
        if(higher <= lower)
            throw new IllegalArgumentException("Invalid range "+lower+":"+ higher);

        int i = new Random().nextInt((higher-lower)) + lower;
        while( ports.contains(i) || isPortInUse(i)) {
            i = new Random().nextInt((higher-lower)) + lower;
        }
        ports.add(i);
        return i;
    }

    private boolean isPortInUse(final int port) throws InterruptedException {
        try {
            return node.getChannel().call(new Callable<Boolean,IOException>() {
                public Boolean call() throws IOException {
                    new ServerSocket(port).close();
                    return true;
                }
            });
        } catch (IOException e) {
            return false;
        }
    }
}
