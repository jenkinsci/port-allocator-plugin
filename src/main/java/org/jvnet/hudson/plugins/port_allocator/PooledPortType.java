package org.jvnet.hudson.plugins.port_allocator;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;

import java.io.IOException;

/**
 * Port type for representing a pool of ports used concurrently by parallel jobs
 *
 * @author pepov
 */
public class PooledPortType extends PortType {

    private int[] pool;

    @DataBoundConstructor
    public PooledPortType(String name) {
        super(name);
    }

    /**
     * Try to allocate one free port from the given pool.
     * Wait for a short period if no free port is available, then try again.
     */
    @Override
    public Port allocate(
        AbstractBuild<?, ?> build,
        final PortAllocationManager manager,
        int prefPort,
        Launcher launcher,
        BuildListener buildListener
    ) throws IOException, InterruptedException {

        try {
            while (true) {

                Pool pool = PortAllocator.DESCRIPTOR.getPoolByName(name);

                synchronized (pool) {
                    for (int port : pool.getPortsAsInt()) {
                        if (manager.isFree(port)) {
                            manager.allocate(build, port);
                            return new PooledPort(this, port, manager);
                        }
                    }
                    pool.wait(500);
                }
            }
        } catch (PoolNotDefinedException e) {
            throw new RuntimeException("Undefined pool: " + name);
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    @Extension
    public static final class DescriptorImpl extends PortTypeDescriptor {

        public DescriptorImpl() {
            super(PooledPortType.class);
        }

        public PooledPortType newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {

            if ("".equals(formData.getString("name"))) {
                throw new FormException(
                    "Unable to setup port allocator for an undefined pool!" +
                      " Please configure pools in the global configuration settings!",
                    "name"
                );
            }

            return new PooledPortType(
                formData.getString("name")
            );
        }

        public String getDisplayName() {
            return "Pooled TCP port";
        }

        public ListBoxModel doFillNameItems() {
            ListBoxModel model = new ListBoxModel();
            for (Pool p : PortAllocator.DESCRIPTOR.getPools()) {
                model.add(p.name, p.name);
            }
            return model;
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }
}
