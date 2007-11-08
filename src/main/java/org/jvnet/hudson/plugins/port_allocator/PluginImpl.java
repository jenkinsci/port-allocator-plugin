package org.jvnet.hudson.plugins.port_allocator;

import hudson.Plugin;
import hudson.tasks.BuildWrappers;

/**
 * PortAllocator Plugin
 * Allocates free ports to the variables.
 *
 * @author Rama Pulavarthi
 */
public class PluginImpl extends Plugin {
    public void start() throws Exception {
        BuildWrappers.WRAPPERS.add(PortAllocator.DESCRIPTOR);
        PortTypeDescriptor.LIST.add(DefaultPortType.DescriptorImpl.INSTANCE);
        PortTypeDescriptor.LIST.add(GlassFishJmxPortType.DescriptorImpl.INSTANCE);
        PortTypeDescriptor.LIST.add(TomcatShutdownPortType.DescriptorImpl.INSTANCE);
    }
}
