package org.jvnet.hudson.plugins.port_allocator;

import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public abstract class Port {
    /**
     * {@link PortType} that created this port.
     */
    public final PortType type;

    protected Port(PortType type) {
        this.type = type;
    }

    /**
     * Gets the TCP port number.
     */
    public abstract int get();

    /**
     * Frees the port.
     */
    public abstract void cleanUp() throws IOException, InterruptedException;
}
