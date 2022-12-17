package org.jvnet.hudson.plugins.port_allocator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

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
     * All registered {@link PortTypeDescriptor}s.
     */
    @SuppressFBWarnings(value = "MS_MUTABLE_COLLECTION_PKGPROTECT",
                        justification = "Low risk to leave it visible outside the package")
    public static final List<PortTypeDescriptor> LIST = new ArrayList<PortTypeDescriptor>();
}
