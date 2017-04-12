package org.jvnet.hudson.plugins.port_allocator;

import net.sf.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;
import org.mockito.Mockito;

import java.util.Collections;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PortAllocatorDescriptorTest {
    
    @Rule
    public JenkinsRule j = new JenkinsRule();
    
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();
    
    private PortAllocator.DescriptorImpl descriptor;
    
    @Before
    public void setUp() {
        descriptor = new PortAllocator.DescriptorImpl();
    }
    
    @Test
    public void test_01_AddAPool() throws Exception {
        Pool pool = new Pool();
        pool.name = "mypool";
        pool.ports = "7001,7002";
    
        StaplerRequest requestMock = Mockito.mock(StaplerRequest.class);
        Mockito.when(requestMock.bindParametersToList(Pool.class, "pool.")).thenReturn(Collections.singletonList(pool));
        JSONObject jsonObject = new JSONObject();
        descriptor.configure(requestMock, jsonObject);
        Assert.assertEquals(1, descriptor.getPools().size());
    }
    
    @Test
    public void test_02_AddAPoolTwice() throws Exception {
        Pool pool = new Pool();
        pool.name = "otherPool";
        pool.ports = "7003,7004";
    
        StaplerRequest requestMock = Mockito.mock(StaplerRequest.class);
        Mockito.when(requestMock.bindParametersToList(Pool.class, "pool.")).thenReturn(Collections.singletonList(pool));
        JSONObject jsonObject = new JSONObject();
        descriptor.configure(requestMock, jsonObject);
        Assert.assertEquals(1, descriptor.getPools().size());
        
        // configure the same pool again. It should yield to the same result
        descriptor.configure(requestMock, jsonObject);
        Assert.assertEquals(1, descriptor.getPools().size());
    }
    
    @Test
    public void test_03_RemoveAPool() throws Exception {
        Pool pool = new Pool();
        pool.name = "wrongPool";
        pool.ports = "7033,7044";
    
        StaplerRequest requestMock = Mockito.mock(StaplerRequest.class);
        
        Mockito.when(requestMock.bindParametersToList(Pool.class, "pool.")).thenReturn(Collections.singletonList(pool));
        JSONObject jsonObject = new JSONObject();
        // add the pool
        descriptor.configure(requestMock, jsonObject);
        Assert.assertEquals(1, descriptor.getPools().size());
    
        // remove the pool
        Mockito.reset(requestMock);
        Mockito.when(requestMock.bindParametersToList(Pool.class, "pool.")).thenReturn(Collections.<Pool>emptyList());
        descriptor.configure(requestMock, jsonObject);
        Assert.assertEquals(0, descriptor.getPools().size());
    }
    
    @Test
    public void test_04_ChangeAPool() throws Exception {
        Pool pool = new Pool();
        pool.name = "wrongPool";
        pool.ports = "7033,7044";
    
        StaplerRequest requestMock = Mockito.mock(StaplerRequest.class);
    
        Mockito.when(requestMock.bindParametersToList(Pool.class, "pool.")).thenReturn(Collections.singletonList(pool));
        JSONObject jsonObject = new JSONObject();
        // add the pool
        descriptor.configure(requestMock, jsonObject);
        Assert.assertEquals(1, descriptor.getPools().size());
        Assert.assertEquals("WRONGPOOL", descriptor.getPools().get(0).name);
        Assert.assertEquals("7033,7044", descriptor.getPools().get(0).ports);
    
        // change the pool
        pool.name = "correctPool";
        pool.ports = "8001,8002,8003,8004";
        Mockito.reset(requestMock);
        Mockito.when(requestMock.bindParametersToList(Pool.class, "pool.")).thenReturn(Collections.singletonList(pool));
        descriptor.configure(requestMock, jsonObject);
        Assert.assertEquals(1, descriptor.getPools().size());
        Assert.assertEquals("CORRECTPOOL", descriptor.getPools().get(0).name);
        Assert.assertEquals("8001,8002,8003,8004", descriptor.getPools().get(0).ports);
    }
}
