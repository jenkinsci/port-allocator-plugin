package org.jvnet.hudson.plugins.port_allocator;

import hudson.Plugin;

/**
 * PortAllocator Plugin
 * Allocates free ports to the variables.
 *
 * @author Rama Pulavarthi
 */
public class PluginImpl extends Plugin {
    @Override
    public void start() throws Exception {
        PortTypeDescriptor.LIST.add(DefaultPortType.DescriptorImpl.INSTANCE);
        PortTypeDescriptor.LIST.add(GlassFishJmxPortType.DescriptorImpl.INSTANCE);
        PortTypeDescriptor.LIST.add(TomcatShutdownPortType.DescriptorImpl.INSTANCE);
    }
}
