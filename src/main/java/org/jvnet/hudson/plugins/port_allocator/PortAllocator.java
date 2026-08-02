package org.jvnet.hudson.plugins.port_allocator;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Allocates TCP Ports on a Computer for consumption and sets it as
 * environment variables, see configuration
 *
 * <p>
 * This just mediates between different Jobs running on the same Computer
 * by assigning free ports and its the jobs responsibility to open and close the ports.   
 *
 * @author Rama Pulavarthi
 */
public class PortAllocator extends BuildWrapper
{
    private static final Log log = LogFactory.getLog(PortAllocator.class);

    public final PortType[] ports;

    private PortAllocator(PortType[] ports){
        this.ports = ports;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();

        final Executor currentExecutor = Executor.currentExecutor();
        if (currentExecutor == null) {
            logger.println("No current executor for port allocator, exiting setUp");
            return new Environment() {
                @Override
                public void buildEnvVars(Map<String, String> env) {
                }
                @Override
                public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                    return true;
                }
            };
        }
        final Computer cur = currentExecutor.getOwner();
        Map<String,Integer> prefPortMap = new HashMap<String,Integer>();
        AbstractBuild previousBuild = build.getPreviousBuild();
        if (previousBuild != null) {
            AllocatedPortAction prevAlloc = previousBuild.getAction(AllocatedPortAction.class);
            if (prevAlloc != null) {
                // try to assign ports assigned in previous build
                prefPortMap = prevAlloc.getPreviousAllocatedPorts();
            }
        }
        final PortAllocationManager pam = PortAllocationManager.getManager(cur);
        Map<String,Integer> portMap = new HashMap<String,Integer>();
        final List<Port> allocated = new ArrayList<Port>();

        for (PortType pt : ports) {
            logger.println("Allocating TCP port "+pt.name);
            int prefPort = prefPortMap.get(pt.name)== null?0:prefPortMap.get(pt.name);
            Port p = pt.allocate(build, pam, prefPort, launcher, listener);
            allocated.add(p);
            portMap.put(pt.name,p.get());
            logger.println("  -> Assigned "+p.get());
        }

        // TODO: only log messages when we are blocking.
        logger.println("TCP port allocation complete");
        build.addAction(new AllocatedPortAction(portMap));

        return new Environment() {

            @Override
            public void buildEnvVars(Map<String, String> env) {
                for (Port p : allocated)
                    env.put(p.type.name, String.valueOf(p.get()));
            }

            @Override
            public boolean tearDown(AbstractBuild build, BuildListener listener) throws IOException, InterruptedException {
                for (Port p : allocated)
                    p.cleanUp();
                return true;
            }
        };
    }

    public String getDisplayName() {
        return "Port exclusion";
    }

    @Override
    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

        private Pool[] pools = new Pool[] {};

        public DescriptorImpl() {
            super(PortAllocator.class);
            load();
        }

        public String getDisplayName() {
            return "Assign unique TCP ports to avoid collisions";
        }

        @Override
        public String getHelpFile() {
            return "/plugin/port-allocator/help.html";
        }

        public List<PortTypeDescriptor> getPortTypes() {
            return PortTypeDescriptor.LIST; 
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
            List<PortType> ports = Descriptor.newInstancesFromHeteroList(
                    req, formData, "ports", PortTypeDescriptor.LIST);

            HashSet<String> portNames = new HashSet<String>();

            for (PortType p : ports) {
                if (!portNames.add(p.name)) {
                    throw new FormException("Cannot add multiple port allocators with the same name!", "name");
                }
            }

            return new PortAllocator(ports.toArray(new PortType[ports.size()]));
        }

        @Override
        public boolean configure(StaplerRequest2 req, JSONObject formData) throws FormException {
            Pool[] pools = req.bindParametersToList(Pool.class, "pool.").toArray(new Pool[] {});
            for (Pool p : pools) {
                p.name = checkPoolName(p.name);
                checkPortNumbers(p.ports);
            }
            this.pools = pools;
            save();
            return super.configure(req,formData);
        }

        public FormValidation doCheckPort(@QueryParameter("value") final String ports) {
            try {
                checkPortNumbers(ports);
                return FormValidation.ok();
            } catch (FormException e) {
                return FormValidation.error(e.getMessage());
            }
        }

        public FormValidation doCheckName(@QueryParameter("value") final String name) {
            try {
                checkPoolName(name);
                return FormValidation.ok();
            } catch (FormException e) {
                return FormValidation.error(e.getMessage());
            }
        }

        private String checkPoolName(String name) throws FormException {

            if ("".equals(name)) {
                throw new FormException("Pool name must not be empty", "name");
            }

            if (!Pattern.matches("\\w+", name)) {
                throw new FormException(
                        "The name: \"" + name + "\" is invalid! It must not contain other than word characters!", "name"
                );
            }

            return name;
        }

        private void checkPortNumbers(String ports) throws FormException {
            if (!Pattern.matches("(\\d+,)*\\d+", ports)) {
                throw new FormException("Need a comma separated list of port numbers", "ports");
            }
        }

        public Pool[] getPools() {
            return pools;
        }

        public Pool getPoolByName(String poolName) throws PoolNotDefinedException {
            for (Pool p : pools) {
                if (p.name.equals(poolName)) {
                    return p;
                }
            }
            throw new PoolNotDefinedException();
        }

        public int getPoolSize(String poolName) throws PoolNotDefinedException {
            return getPoolByName(poolName).getPortsAsInt().length;
        }
    }
}
