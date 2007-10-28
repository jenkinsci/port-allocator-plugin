package org.jvnet.hudson.plugins.port_allocator;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import hudson.model.AbstractBuild;

import java.io.IOException;

import net.sf.json.JSONObject;

/**
 * @author Kohsuke Kawaguchi
 */
public class GlassFishJmxPortType extends PortType {
    private final String userName;
    private final String password;

    @DataBoundConstructor
    public GlassFishJmxPortType(String name, String userName, String password) {
        super(name);
        this.userName = userName;
        this.password = password;
    }

    @Override
    public Port allocate(AbstractBuild<?,?> build, final PortAllocationManager manager, int prefPort) throws IOException, InterruptedException {
        final int n = manager.allocateRandom(build, prefPort);
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
            super(GlassFishJmxPortType.class);
        }

        public GlassFishJmxPortType newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // TODO: we need form binding from JSON
            return new GlassFishJmxPortType(
                formData.getString("name"),
                formData.getString("username"),
                formData.getString("password"));
        }

        public String getDisplayName() {
            return "GlassFish JMX port";
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }
}

