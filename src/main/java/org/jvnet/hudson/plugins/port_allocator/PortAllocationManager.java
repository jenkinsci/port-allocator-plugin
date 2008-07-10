package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.AbstractBuild;
import hudson.model.Computer;
import hudson.remoting.Callable;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;


/**
 * Manages ports in use.
 *
 * @author Rama Pulavarthi
 * @author Kohsuke Kawaguchi
 */
final class PortAllocationManager {
    private final Computer node;


    /**
     * Ports currently in use, to the build that uses it.
     */
    private final Map<Integer,AbstractBuild> ports = new HashMap<Integer,AbstractBuild>();

    private static final Map<Computer, WeakReference<PortAllocationManager>> INSTANCES =
            new WeakHashMap<Computer, WeakReference<PortAllocationManager>>();

    private PortAllocationManager(Computer node) {
        this.node = node;
    }

    /**
     * Allocates a random port on the Computer where the jobs gets executed.
     *
     * <p>
     * If the preferred port is not available, assigns a random available port.
     *
     * @param prefPort Preffered Port
     */
    public synchronized int allocateRandom(AbstractBuild owner, int prefPort) throws InterruptedException, IOException {
        int i;
        try {
            // try to allocate preferential port,
            i = allocatePort(prefPort);
        } catch (PortUnavailableException ex) {
            // if not available, assign a random port
            i = allocatePort(0);
        }
        ports.put(i,owner);
        return i;
    }

    /**
     * Assigns the requested port.
     *
     * This method blocks until the port becomes available.
     */
    public synchronized int allocate(AbstractBuild owner, int port) throws InterruptedException, IOException {
        while(ports.get(port)!=null)
            wait();

        /*
        TODO:
            despite the fact that the following commented out implementation is smarter,
            I need to comment it out for the following reasons:

            Often, a job that requires a particular port (let's say 8080) fails in the middle,
            leaving behind a process that occupies the port. A typical defensive measure taken
            to fight this is for the build to try to stop the server at the beginning.

            While clunky, this technique is used frequently, and if this method blocks until
            the port actually becomes available (not only in our book-keeping but also at OS level),
            then such a run-away process will remain forever and thus the next build will
            never happen.

            This unfortunately doesn't protect us from a case where job X runs and dies, leaving
            a server behind, then job Y comes in, looking for the same port, and fails.

            We need to revisit this.
         */
//        while(true) {
//            try {
//                allocatePort(port);
//                break;
//            } catch (PortUnavailableException e) {
//                // the requested port is not available right now.
//                // the port might be blocked by a reason outside Hudson,
//                // so we need to occasionally wake up to see if the port became available
//                wait(10000);
//            }
//        }
        ports.put(port,owner);
        return port;
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
        ports.remove(n);
        notifyAll(); // wake up anyone who's waiting for this port
    }


//    private boolean isPortInUse(final int port) throws InterruptedException {
//        try {
//            return node.getChannel().call(new Callable<Boolean, IOException>() {
//                public Boolean call() throws IOException {
//                    new ServerSocket(port).close();
//                    return true;
//                }
//            });
//        } catch (IOException e) {
//            return false;
//        }
//    }

    /**
     * @param port 0 to assign a free port
     * @return port that gets assigned
     * @throws PortUnavailableException
     *      If the specified port is not availabale
     */
    private int allocatePort(final int port) throws InterruptedException, IOException {
        AbstractBuild owner = ports.get(port);
        if(owner!=null)
            throw new PortUnavailableException("Owned by "+owner);

        return node.getChannel().call(new AllocateTask(port));
    }

    static final class PortUnavailableException extends IOException {
        PortUnavailableException(String msg) {
            super(msg);
        }

        // not compatible with JDK1.5
//        PortUnavailableException(Throwable cause) {
//            super(cause);
//        }

        private static final long serialVersionUID = 1L;
    }

    private static final class AllocateTask implements Callable<Integer,IOException> {
        private final int port;

        public AllocateTask(int port) {
            this.port = port;
        }

        public Integer call() throws IOException {
            ServerSocket server;
            try {
                server = new ServerSocket(port);
            } catch (IOException e) {
                // fail to bind to the port
                PortUnavailableException t = new PortUnavailableException(e.getLocalizedMessage());
                t.initCause(e);
                throw t;
            }
            int localPort = server.getLocalPort();
            server.close();
            return localPort;
        }

        private static final long serialVersionUID = 1L;
    }
}
