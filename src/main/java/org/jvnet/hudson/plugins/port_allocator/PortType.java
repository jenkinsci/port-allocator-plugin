package org.jvnet.hudson.plugins.port_allocator;

import hudson.ExtensionPoint;
import hudson.model.AbstractBuild;
import hudson.model.Describable;

import java.io.IOException;

/**
 * Base class for different types of TCP port.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class PortType implements ExtensionPoint, Describable<PortType> {
    /**
     * Name that identifies {@link PortType} among other {@link PortType}s in the
     * same {@link PortAllocator}.
     */
    public final String name;

    protected PortType(String name) {
        // to avoid platform difference issue in case sensitivity of environment variables,
        // always use uppser case.
        this.name = name.toUpperCase();
    }

    /**
     * Allocates a new port for a given build.
     *
     * @param manager
     *      This can be used to assign a new TCP port number.
     * @param prefPort
     *      The port number allocated to this type the last time.
     *      Implementation is encouraged to use the same port number again.
     */
    public abstract Port allocate(AbstractBuild<?,?> build, PortAllocationManager manager, int prefPort) throws IOException, InterruptedException;

    public abstract PortTypeDescriptor getDescriptor();
}
