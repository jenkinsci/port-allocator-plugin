package org.jvnet.hudson.plugins.port_allocator;

/**
 * Represents a port pool.
 *
 * @author pepov
 */
public class Pool {
    
    public String name;
    public String ports;
    public boolean isGlobal;

    public int[] getPortsAsInt() {

        String[] portsItemsAsString = ports.split(",");

        int[] portsItems = new int[portsItemsAsString.length];

        for (int i = 0; i < portsItemsAsString.length; i++) {
            portsItems[i] = Integer.parseInt(portsItemsAsString[i]);
        }

        return portsItems;
    }
}
