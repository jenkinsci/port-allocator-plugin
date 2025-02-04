package org.jvnet.hudson.plugins.port_allocator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.remoting.Callable;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest2;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
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
    public Port allocate(AbstractBuild<?, ?> build, final PortAllocationManager manager, int prefPort, final Launcher launcher, final BuildListener buildListener) throws IOException, InterruptedException {
        final int n;
        if(isFixedPort())
            n = manager.allocate(build, getFixedPort());
        else
            n = manager.allocateRandom(build, prefPort);

        final class TomcatCleanUpTask implements Callable<Void,IOException>, Serializable {
            private final BuildListener buildListener;

            public TomcatCleanUpTask(BuildListener buildListener) {
                this.buildListener = buildListener;
            }

            @Override
            public void checkRoles(final org.jenkinsci.remoting.RoleChecker checker) throws SecurityException {
                checker.check(this, jenkins.security.Roles.SLAVE);
            }

            @SuppressFBWarnings(value = "DM_DEFAULT_ENCODING",
                                justification = "Read UTF-8 as first preference")
            private byte[] getBytes(String s) {
                byte[] bytes;
                try {
                    return s.getBytes("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return s.getBytes();
                }
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
                    s.getOutputStream().write(getBytes(password));
                    s.close();
                    buildListener.getLogger().println("Shutdown left-over Tomcat");
                } catch (IOException x) {
                    x.printStackTrace(buildListener.error("Failed to write to Tomcat shutdown port"));
                }
                return null;
            }

            @Serial
            private static final long serialVersionUID = 1L;
        }

        return new Port(this) {
            public int get() {
                return n;
            }

            public void cleanUp() throws IOException, InterruptedException {
                manager.free(n);
                hudson.remoting.VirtualChannel channel = launcher.getChannel();
                if (channel != null) {
                    channel.call(new TomcatCleanUpTask(buildListener));
                }
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

        public TomcatShutdownPortType newInstance(StaplerRequest2 req, JSONObject formData) throws FormException {
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

    @Serial
    private static final long serialVersionUID = 1L;
}

