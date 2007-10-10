package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.Action;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Kohsuke Kawaguchi
 */
public class AllocatedPortAction implements Action {
    private final Map<String, Integer> portMap = new HashMap<String,Integer>();

    public AllocatedPortAction(Map<String, Integer> portMap) {
        this.portMap.putAll(portMap);
    }

    public String getIconFileName() {
        return null;
    }

    public String getDisplayName() {
        return null;
    }

    public String getUrlName() {
        return "allocatedPorts";
    }

    Map<String, Integer> getPreviousAllocatedPorts() {
        return portMap;
    }
}
