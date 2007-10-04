package org.jvnet.hudson.plugins.port_allocator;

import junit.framework.Assert;

import java.util.Set;
import java.util.HashSet;
import java.util.Random;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Manages ports in use.
 *
 * @author Rama Pulavarthi
 */
final class PortAllocationManager {
    /**
     * Ports currently in use.
     */
    private final Set<Integer> ports = new HashSet<Integer>();

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
        if(!isPortInUse(null,n)) {
            ports.remove(n);
        }
    }

    private static int findFreePort()
            throws IOException {
        ServerSocket server =
                new ServerSocket(0);
        int port = server.getLocalPort();
        server.close();
        return port;
    }

    private synchronized int allocateInRange(int lower, int higher) {
        if(higher <= lower)
            throw new IllegalArgumentException("Invalid range "+lower+":"+ higher);

        int i = new Random().nextInt(lower) + (higher-lower);
        while( ports.contains(i) || isPortInUse(null,i)) {
            i = new Random().nextInt(lower) + (higher-lower);
        }
        ports.add(i);
        return i;
    }

    private boolean isPortInUse(String host, int port) {
        try {
            Socket s = new Socket(host, port);
        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
