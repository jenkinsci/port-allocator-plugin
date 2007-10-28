package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.Launcher;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;

/**
 * Plain TCP port that doesn't have
 * any special clean up processing.
 *
 * @author Kohsuke Kawaguchi
 */
public class DefaultPortType extends PortType {
    @DataBoundConstructor
    public DefaultPortType(String name) {
        super(name);
    }

    @Override
    public Port allocate(AbstractBuild<?, ?> build, final PortAllocationManager manager, int prefPort, Launcher launcher, BuildListener buildListener) throws IOException, InterruptedException {
        final int n;
        if(isFixedPort())
            n = manager.allocate(build, getFixedPort());
        else
            n = manager.allocateRandom(build, prefPort);
        
        return new Port(this) {
            public int get() {
                return n;
            }

            public void cleanUp() {
                manager.free(n);
            }
        };
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    public static final class DescriptorImpl extends PortTypeDescriptor {
        private DescriptorImpl() {
            super(DefaultPortType.class);
        }

        public DefaultPortType newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // TODO: we need form binding from JSON
            return new DefaultPortType(formData.getString("name"));
        }

        public String getDisplayName() {
            return "Plain TCP port";
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }
}
