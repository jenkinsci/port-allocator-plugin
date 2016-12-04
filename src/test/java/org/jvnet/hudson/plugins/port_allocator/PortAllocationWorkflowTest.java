package org.jvnet.hudson.plugins.port_allocator;

import hudson.model.Label;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.DumbSlave;
import hudson.slaves.NodeProperty;
import hudson.slaves.RetentionStrategy;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.MethodSorters;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.RestartableJenkinsRule;

import java.util.Collections;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PortAllocationWorkflowTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void wrap_01_WithNonExistingPool() throws Exception {
        j.jenkins.addNode(new DumbSlave("slave", "dummy",
            tmp.newFolder("remoteFS").getPath(), "1", Node.Mode.NORMAL, "",
            j.createComputerLauncher(null), RetentionStrategy.NOOP, Collections.<NodeProperty<?>>emptyList()));
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node('slave') {\n"
                + "  wrap([$class: 'PortAllocator', pools: ['WEBLOGIC']]) {\n"
                + "  }\n"
                + "}"
        ));
        j.assertBuildStatus(Result.FAILURE, p.scheduleBuild2(0).get());
    }

    @Test
    public void wrap_02_WithExistingPool() throws Exception {
        j.jenkins.addNode(new DumbSlave("slave", "dummy",
            tmp.newFolder("remoteFS").getPath(), "1", Node.Mode.NORMAL, "",
            j.createComputerLauncher(null), RetentionStrategy.NOOP, Collections.<NodeProperty<?>>emptyList()));
        PortAllocator.DescriptorImpl desc = j.jenkins.getDescriptorByType(PortAllocator.DescriptorImpl.class);
        Pool weblogic = new Pool();
        weblogic.name = "WEBLOGIC";
        weblogic.ports = "7001,8001";
        desc.getPools().add(weblogic);
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node('slave') {\n"
                + "  wrap([$class: 'PortAllocator', pools: ['WEBLOGIC']]) {\n"
                + "  }\n"
                + "}"
        ));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    }

    @Test
    public void wrap_03_WithExistingLegacyPool() throws Exception {
        j.jenkins.addNode(new DumbSlave("slave", "dummy",
            tmp.newFolder("remoteFS").getPath(), "1", Node.Mode.NORMAL, "",
            j.createComputerLauncher(null), RetentionStrategy.NOOP, Collections.<NodeProperty<?>>emptyList()));
        PortAllocator.DescriptorImpl desc = j.jenkins.getDescriptorByType(PortAllocator.DescriptorImpl.class);
        Pool weblogic = new Pool();
        weblogic.name = "WEBLOGIC";
        weblogic.ports = "7001,8001";
        desc.getPools().add(weblogic);
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node('slave') {\n"
                + "  wrap([$class: 'PortAllocator', pool: 'WEBLOGIC']) {\n"
                + "  }\n"
                + "}"
        ));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    }

    @Test
    public void wrap_04_WithPlainPort() throws Exception {
        j.jenkins.addNode(new DumbSlave("slave", "dummy",
            tmp.newFolder("remoteFS").getPath(), "1", Node.Mode.NORMAL, "",
            j.createComputerLauncher(null), RetentionStrategy.NOOP, Collections.<NodeProperty<?>>emptyList()));
        PortAllocator.DescriptorImpl desc = j.jenkins.getDescriptorByType(PortAllocator.DescriptorImpl.class);
        WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
            "node('slave') {\n"
                + "  wrap([$class: 'PortAllocator', plainports: ['PLAINPORT']]) {\n"
                + "  }\n"
                + "}"
        ));
        j.assertBuildStatusSuccess(p.scheduleBuild2(0).get());
    }

}
