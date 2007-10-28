package org.jvnet.hudson.plugins.port_allocator;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;
import javax.management.MBeanException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.EOFException;
import java.io.IOException;
import java.rmi.UnmarshalException;
import java.util.HashMap;
import java.util.Map;

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
    public Port allocate(AbstractBuild<?, ?> build, final PortAllocationManager manager, int prefPort, final Launcher launcher, final BuildListener buildListener) throws IOException, InterruptedException {
        final int n = manager.allocateRandom(build, prefPort);
        return new Port(this) {
            public int get() {
                return n;
            }

            public void cleanUp() throws IOException, InterruptedException {
                manager.free(n);

                launcher.getChannel().call(new Callable<Void,IOException>() {
                    public Void call() throws IOException {
                        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:"+n+"/jmxrmi");

                        Map<String,Object> envs = new HashMap<String,Object>();
                        envs.put(JMXConnector.CREDENTIALS,new String[]{userName, password});

                        MBeanServerConnection con = JMXConnectorFactory.connect(url,envs).getMBeanServerConnection();
                        try {
                            con.invoke(new ObjectName("amx:j2eeType=J2EEServer,name=server"),"stop",new Object[0],new String[0]);
                        } catch (UnmarshalException e) {
                            if(e.getCause() instanceof EOFException) {
                                // to be expected, as the above would shut down the server.
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
                });
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

