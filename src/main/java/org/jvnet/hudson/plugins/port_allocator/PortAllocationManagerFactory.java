package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.Computer;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

public class PortAllocationManagerFactory {

  public static final ClusterPortAllocationManager CLUSTER_INSTANCE = new ClusterPortAllocationManager();

  private static final Map<Computer, WeakReference<NodePortAllocationManager>> INSTANCES =
          new WeakHashMap<Computer, WeakReference<NodePortAllocationManager>>();

  public static NodePortAllocationManager getManager(Computer node) {
    NodePortAllocationManager pam;
    WeakReference<NodePortAllocationManager> ref = INSTANCES.get(node);
    if (ref != null) {
      pam = ref.get();
      if (pam != null)
        return pam;
    }

    pam = new NodePortAllocationManager(node);

    INSTANCES.put(node, new WeakReference<NodePortAllocationManager>(pam));

    return pam;
  }

}
