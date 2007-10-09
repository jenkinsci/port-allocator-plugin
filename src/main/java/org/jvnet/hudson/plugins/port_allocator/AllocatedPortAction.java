package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.Action;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class AllocatedPortAction implements Action {
    public AllocatedPortAction(Map<String, Integer> portMap) {
        // TODO
    }

    public String getIconFileName() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public String getDisplayName() {
        // TODO
        throw new UnsupportedOperationException();
    }

    public String getUrlName() {
        // TODO
        throw new UnsupportedOperationException();
    }
}
