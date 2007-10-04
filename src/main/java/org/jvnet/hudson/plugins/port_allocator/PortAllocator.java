package org.jvnet.hudson.plugins.port_allocator;

import hudson.Launcher;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.Computer;
import hudson.tasks.BuildWrapper;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class PortAllocator extends BuildWrapper
{
    private final HashMap<String,Integer> portMap = new HashMap<String,Integer>();
    private final String portVariables;

    public PortAllocator(String portVariables){
        this.portVariables = portVariables;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getPortVariables() {
        return portVariables;
    }

    public Environment setUp(Build build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        final String[] portVars = portVariables.split(" ");

        Computer cur = Executor.currentExecutor().getOwner();

        build.addAction();

        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                for(String portVar: portVars){
                    int freeport = portManager.allocate();
                    portMap.put(portVar, freeport);
                    //set the environment variable
                    env.put(portVar, String.valueOf(freeport));
                }

            }

            public boolean tearDown(Build build, BuildListener listener) throws IOException, InterruptedException {
                for(String portVar: portVars){
                    portManager.free(portMap.get(portVar));
                    portMap.remove(portVar);
                }
                return true;
            }
        };
    }

    public Descriptor<BuildWrapper> getDescriptor() {
        return DESCRIPTOR;
    }


    /**
     * Manages ports in use.
     */
    private static final PortAllocationManager portManager = new PortAllocationManager();

    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<BuildWrapper> {

        DescriptorImpl() {
            super(PortAllocator.class);
            load();
        }

        public String getDisplayName() {
            return "Run Port Allocator during build";
        }

        public String getHelpFile() {
            return "/plugin/port-allocator/help.html";
        }

        public boolean configure(StaplerRequest req) throws FormException {
            req.bindParameters(this,"portallocator.");
            save();
            return true;
        }

        public PortAllocator newInstance(StaplerRequest req) throws FormException {
            return new PortAllocator(req.getParameter("portallocator.portVariables"));
        }

        private static final long serialVersionUID = 1L;
    }

}
