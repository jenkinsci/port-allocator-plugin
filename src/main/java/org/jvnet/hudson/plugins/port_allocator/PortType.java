package org.jvnet.hudson.plugins.port_allocator;

import hudson.ExtensionPoint;
import hudson.Launcher;
import hudson.model.Describable;
import hudson.model.BuildListener;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;
import java.io.Serializable;

/**
 * Base class for different types of TCP port.
 *
 * <p>
 * This class implements {@link Serializable} so that the clean up task to be executed
 * remotely can drag this class into the serialization graph.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class PortType implements ExtensionPoint, Describable<PortType>, Serializable {
    /**
     * Name that identifies {@link PortType} among other {@link PortType}s in the
     * same {@link PortAllocator}, or the numeric port number value if that port
     * number is fixed.
     */
    public final String name;

    protected PortType(String name) {
        // to avoid platform difference issue in case sensitivity of environment variables,
        // always use uppser case.
        this.name = name.toUpperCase();
    }

    /**
     * If this port type has a fixed port number, return that value.
     * Otherwise 0.
     */
    public final int getFixedPort() {
        try {
            return Integer.parseInt(name);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Returns true if this port type has a fixed port number.
     */
    public final boolean isFixedPort() {
        return getFixedPort()!=0;
    }

    /**
     * Allocates a new port for a given build.
     * @param run
     *      The current build
     * @param manager
     *      This can be used to assign a new TCP port number.
     * @param prefPort
     *  The port number allocated to this type the last time.
     * @param launcher
     */
    public abstract Port allocate(Run<?, ?> run, PortAllocationManager manager, int prefPort, Launcher launcher, TaskListener taskListener) throws IOException, InterruptedException;

    public abstract PortTypeDescriptor getDescriptor();

    private static final long serialVersionUID = 1L;
}
