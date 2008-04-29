package org.jvnet.hudson.plugins.port_allocator;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Computer;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildWrapper;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Allocates TCP Ports on a Computer for consumption and sets it as
 * envioronet variables, see configuration
 *
 * <p>
 * This just mediates between different Jobs running on the same Computer
 * by assigning free ports and its the jobs responsibility to open and close the ports.   
 *
 * <p>
 * TODO: implement ResourceActivity so that the queue performs intelligent job allocations
 * based on the port availability, instead of start executing something then block.
 *
 * @author Rama Pulavarthi
 */
public class PortAllocator extends BuildWrapper /* implements ResourceActivity */
{
    public final PortType[] ports;

    private PortAllocator(PortType[] ports){
        this.ports = ports;
    }

    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        PrintStream logger = listener.getLogger();

        final Computer cur = Executor.currentExecutor().getOwner();
        Map<String,Integer> prefPortMap = new HashMap<String,Integer>();
        if (build.getPreviousBuild() != null) {
            AllocatedPortAction prevAlloc = build.getPreviousBuild().getAction(AllocatedPortAction.class);
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

            public boolean tearDown(Build build, BuildListener listener) throws IOException, InterruptedException {
                for (Port p : allocated)
                    p.cleanUp();
                return true;
            }
        };
    }

    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }


    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {
        DescriptorImpl() {
            super(PortAllocator.class);
            load();
        }

        public String getDisplayName() {
            return "Assign unique TCP ports to avoid collisions";
        }

        public String getHelpFile() {
            return "/plugin/port-allocator/help.html";
        }

        public List<PortTypeDescriptor> getPortTypes() {
            return PortTypeDescriptor.LIST; 
        }

        @Override
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            List<PortType> ports = Descriptor.newInstancesFromHeteroList(
                    req, formData, "ports", PortTypeDescriptor.LIST);
            return new PortAllocator(ports.toArray(new PortType[ports.size()]));
        }
    }

}
