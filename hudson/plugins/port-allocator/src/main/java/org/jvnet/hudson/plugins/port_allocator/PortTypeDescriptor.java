package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.Descriptor;
import org.kohsuke.stapler.StaplerRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link Descriptor} for {@link PortType}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class PortTypeDescriptor extends Descriptor<PortType> {
    protected PortTypeDescriptor(Class<? extends PortType> clazz) {
        super(clazz);
    }

    /**
     * Unused.
     */
    @Override
    public final PortType newInstance(StaplerRequest req) throws FormException {
        throw new UnsupportedOperationException();
    }

    /**
     * All registered {@link PortTypeDescriptor}s.
     */
    public static final List<PortTypeDescriptor> LIST = new ArrayList<PortTypeDescriptor>();
}
