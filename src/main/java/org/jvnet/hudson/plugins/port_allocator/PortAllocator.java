package org.jvnet.hudson.plugins.port_allocator;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildWrapper;
import jenkins.tasks.SimpleBuildWrapper;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.google.common.collect.Lists;

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
public class PortAllocator extends SimpleBuildWrapper
{
    private static final Log log = LogFactory.getLog(PortAllocator.class);

    public final List<PortType> ports = Lists.newArrayList();

	private String pool;
	private final List<String> pools = Lists.newArrayList();
	private final List<String> plainports = Lists.newArrayList();

	private PortAllocator(PortType[] ports) {
		this.ports.addAll(Arrays.asList(ports));
	}

	@DataBoundConstructor
	public PortAllocator() {

	}

	@DataBoundSetter
	public void setPool(String pool) {
		if (pool != null) {
			this.ports.add(new PooledPortType(pool));
			this.pool = pool;
		}
	}

	public String getPool() {
		return this.pool;
	}

	@DataBoundSetter
	public void setPools(String[] pools) {
		if (pools != null) {
			for (String pool : pools) {
				this.ports.add(new PooledPortType(pool));
				this.pools.add(pool);
			}
		}
	}

	public String[] getPools() {
		return this.pools.toArray(new String[this.pools.size()]);
	}

	@DataBoundSetter
	public void setPlainports(String[] plainports) {
		if (plainports != null) {
			for (String port : plainports) {
				this.ports.add(new DefaultPortType(port));
				this.plainports.add(port);
			}
		}
	}

	public String[] getPlainports() {
		return this.plainports.toArray(new String[this.plainports.size()]);
	}

    @Override
    public void setUp(Context context, Run<?, ?> run, FilePath workspace, Launcher launcher, TaskListener taskListener, EnvVars envVars) throws IOException, InterruptedException {
        PrintStream logger = taskListener.getLogger();

        Computer cur = workspace.toComputer();
        Map<String,Integer> prefPortMap = new HashMap<String,Integer>();
        if (run.getPreviousBuild() != null) {
            AllocatedPortAction prevAlloc = run.getPreviousBuild().getAction(AllocatedPortAction.class);
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
            Port p = pt.allocate(run, pam, prefPort, launcher, taskListener);
            allocated.add(p);
            portMap.put(pt.name, p.get());
            logger.println("  -> Assigned "+p.get());
        }

        // TODO: only log messages when we are blocking.
        logger.println("TCP port allocation complete");
        run.addAction(new AllocatedPortAction(portMap));

        context.setDisposer(new CleanupDisposer(allocated));
        for (Port p : allocated)
            context.env(p.type.name, String.valueOf(p.get()));
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

        private List<Pool> pools = new ArrayList<Pool>();

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
        public BuildWrapper newInstance(StaplerRequest req, JSONObject formData) throws FormException {
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
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            Pool[] pools = req.bindParametersToList(Pool.class, "pool.").toArray(new Pool[] {});
            this.pools.clear();
            for (Pool p : pools) {
                p.name = checkPoolName(p.name);
                checkPortNumbers(p.ports);
                this.pools.add(p);
            }
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

            if (Pattern.matches("\\w+", name)) {
                return name.toUpperCase();
            } else {
                throw new FormException(
                    "The name: \"" + name + "\" is invalid! It must not contain other than word characters!", "name"
                );
            }
        }

        private void checkPortNumbers(String ports) throws FormException {
            if (!Pattern.matches("(\\d+,)*\\d+", ports)) {
                throw new FormException("Need a comma separated list of port numbers", "ports");
            }
        }

        public List<Pool> getPools() {
            return pools;
        }

        public Pool getPoolByName(String poolName) throws PoolNotDefinedException {
            for (Pool p : pools) {
                if (p.name.toUpperCase().equals(poolName.toUpperCase())) {
                    return p;
                }
            }
            throw new PoolNotDefinedException();
        }

        public int getPoolSize(String poolName) throws PoolNotDefinedException {
            return getPoolByName(poolName).getPortsAsInt().length;
        }
    }

    private static class CleanupDisposer extends Disposer {

        List<Port> allocated;

        public CleanupDisposer(List<Port> allocated) {
            this.allocated = allocated;
        }

        @Override
        public void tearDown(Run<?, ?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws IOException, InterruptedException {
            for (Port p : allocated)
                p.cleanUp();
        }
    }
}
