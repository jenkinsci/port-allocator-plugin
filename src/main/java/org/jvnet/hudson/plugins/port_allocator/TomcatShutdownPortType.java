package org.jvnet.hudson.plugins.port_allocator;

import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import net.sf.json.JSONObject;
import org.jenkinsci.remoting.RoleChecker;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;

/**
 * Tomcat shutdown port.
 *
 * @author Kohsuke Kawaguchi
 */
public class TomcatShutdownPortType  extends PortType {
    /**
     * Shutdown magic phrase.
     */
    public final String password;

    @DataBoundConstructor
    public TomcatShutdownPortType(String name, String password) {
        super(name);
        this.password = password;
    }

    @Override
    public Port allocate(Run<?, ?> run, final PortAllocationManager manager, int prefPort, final Launcher launcher, final TaskListener taskListener)
        throws IOException, InterruptedException
    {
        final int n;
        if(isFixedPort())
            n = manager.allocate(run, getFixedPort());
        else
            n = manager.allocateRandom(run, prefPort);

        final class TomcatCleanUpTask implements Callable<Void,IOException>, Serializable {
            private final TaskListener taskListener;

            public TomcatCleanUpTask(TaskListener taskListener) {
                this.taskListener = taskListener;
            }

            public Void call() throws IOException {
                Socket s;
                try {
                    s = new Socket("localhost",n);
                } catch (IOException x) {
                    // failed to connect. It's not running.
                    return null;
                }

                try {
                    s.getOutputStream().write(password.getBytes());
                    s.close();
                    taskListener.getLogger().println("Shutdown left-over Tomcat");
                } catch (IOException x) {
                    x.printStackTrace(taskListener.error("Failed to write to Tomcat shutdown port"));
                }
                return null;
            }

            private static final long serialVersionUID = 1L;

            @Override
            public void checkRoles(RoleChecker roleChecker) throws SecurityException {
            }
        }

        return new Port(this) {
            public int get() {
                return n;
            }

            public void cleanUp() throws IOException, InterruptedException {
                manager.free(n);
                launcher.getChannel().call(new TomcatCleanUpTask(taskListener));
            }
        };
    }

    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.INSTANCE;
    }

    public static final class DescriptorImpl extends PortTypeDescriptor {
        private DescriptorImpl() {
            super(TomcatShutdownPortType.class);
        }

        public TomcatShutdownPortType newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            // TODO: we need form binding from JSON
            return new TomcatShutdownPortType(
                formData.getString("name"),
                formData.getString("password"));
        }

        public String getDisplayName() {
            return "Tomcat shutdown port";
        }

        public static final DescriptorImpl INSTANCE = new DescriptorImpl();
    }

    private static final long serialVersionUID = 1L;
}

