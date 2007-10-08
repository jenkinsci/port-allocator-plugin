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
     * @param port
     * @return a free port if port =0
     *          else assigns the requested port if free.
     */
    public synchronized int allocate(int port) {
        try {
            int i = allocatePort(port);
            if(port != 0) {
                while(ports.contains(i)) {
                    //alocate new port.
                    i = allocatePort(port);
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
        WeakReference<PortAllocationManager> ref= INSTANCES.get(node);
        if( ref != null) {
            pam = ref.get();
            if(pam != null)
                return pam;
        }
        pam = new PortAllocationManager(node);
        INSTANCES.put(node, new WeakReference<PortAllocationManager>(pam));
        return pam;
    }

    public synchronized void free(int n) {
        //Check if port is still in use
        try {
            if(!isPortInUse(n)) {
                ports.remove(n);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    /*
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
    */
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

    /**
     *
     * @param port 0 to assign a free port
     * @return port that gets assigned
     * @throws InterruptedException
     * @throws IOException
     */
    private int allocatePort(final int port) throws InterruptedException, IOException {
        return node.getChannel().call(new Callable<Integer,IOException>() {
                public Integer call() throws IOException {
                    ServerSocket server = new ServerSocket(port);
                    int localPort = server.getLocalPort();
                    server.close();
                    return localPort;
                }
            });
    }
}
