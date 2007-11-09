package org.jvnet.hudson.plugins.port_allocator;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.SocketException;
import java.rmi.UnmarshalException;
import java.util.HashMap;
import java.util.Map;

/**
 * GlassFish JMX port so that runaway GF instance can be terminated.
 * 
 * @author Kohsuke Kawaguchi
 */
public class GlassFishJmxPortType extends PortType {
    /**
     * GlassFish admin user name.
     */
    public final String userName;
    /**
     * GlassFish admin password.
     */
    public final String password;

    @DataBoundConstructor
    public GlassFishJmxPortType(String name, String userName, String password) {
        super(name);
        this.userName = userName;
        this.password = password;
    }

    @Override
    public Port allocate(AbstractBuild<?, ?> build, final PortAllocationManager manager, int prefPort, final Launcher launcher, final BuildListener buildListener) throws IOException, InterruptedException {
        final int n;
        if(isFixedPort())
            n = manager.allocate(build, getFixedPort());
        else
            n = manager.allocateRandom(build, prefPort);

        /**
         * Cleans up GlassFish instance.
         */
        final class GlassFishCleanUpTask implements Callable<Void,IOException>, Serializable {
            private final BuildListener buildListener;

            public GlassFishCleanUpTask(BuildListener buildListener) {
                this.buildListener = buildListener;
            }

            public Void call() throws IOException {
                JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:"+ n +"/jmxrmi");

                Map<String,Object> envs = new HashMap<String,Object>();
                envs.put(JMXConnector.CREDENTIALS,new String[]{userName, password});

                MBeanServerConnection con;
                try {
                    con = JMXConnectorFactory.connect(url,envs).getMBeanServerConnection();
                } catch (IOException e) {
                    for(Throwable t=e; t!=null; t=t.getCause())
                        if(t instanceof ConnectException) {
                            // server not connectable. must have been shut down already
                            return null;
                        }
                    throw e; // other failure
                }
                try {
                    con.invoke(new ObjectName("amx:j2eeType=J2EEServer,name=server"),"stop",new Object[0],new String[0]);
                } catch (UnmarshalException e) {
                    if(e.getCause() instanceof SocketException || e.getCause() instanceof IOException) {
                        // to be expected, as the above would shut down the server.
                        buildListener.getLogger().println("GlassFish was shut down");
                    } else {
                        throw e;
                    }
                } catch (MalformedObjectNameException e) {
                    throw new AssertionError(e); // impossible
                } catch (InstanceNotFoundException e) {
                    e.printStackTrace(buildListener.error("Unable to find J2EEServer mbean"));
                } catch (ReflectionException e) {
                    e.printStackTrace(buildListener.error("Unable to access J2EEServer mbean"));
                } catch (MBeanException e) {
                    e.printStackTrace(buildListener.error("Unable to call J2EEServer mbean"));
                }
                return null;
            }

            private static final long serialVersionUID = 1L;
        }

        return new Port(this) {
            public int get() {
                return n;
            }

            public void cleanUp() throws IOException, InterruptedException {
                manager.free(n);
                launcher.getChannel().call(new GlassFishCleanUpTask(buildListener));
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

    private static final long serialVersionUID = 1L;
}

