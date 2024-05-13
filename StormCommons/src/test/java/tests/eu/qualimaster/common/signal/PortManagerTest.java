/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tests.eu.qualimaster.common.signal;

import java.io.File;
import java.util.Set;

import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.storm.curator.framework.CuratorFrameworkFactory;
import org.apache.storm.curator.retry.RetryNTimes;
import org.junit.Assert;
import org.junit.Test;

import backtype.storm.LocalCluster;
import eu.qualimaster.common.signal.PortManager;
import eu.qualimaster.common.signal.PortManager.IPortAssignmentWatcher;
import eu.qualimaster.common.signal.PortManager.PortAssignment;
import eu.qualimaster.common.signal.PortManager.PortAssignmentRequest;
import eu.qualimaster.common.signal.PortManager.PortRange;
import eu.qualimaster.common.signal.SignalException;
import eu.qualimaster.common.signal.SignalMechanism;
import tests.eu.qualimaster.TestHelper;

/**
 * Implements tests for the port manager.
 * 
 * @author Holger Eichelberger
 */
public class PortManagerTest {

    /**
     * Tests port range instances.
     */
    @Test
    public void testPortRange() {
        PortRange range = new PortRange(10, 1000);
        Assert.assertEquals(10, range.getLowPort());
        Assert.assertEquals(1000, range.getHighPort());
        
        range = new PortRange(1000, 10);
        Assert.assertEquals(10, range.getLowPort());
        Assert.assertEquals(1000, range.getHighPort());
        
        range = new PortRange("10 - 1000");
        Assert.assertEquals(10, range.getLowPort());
        Assert.assertEquals(1000, range.getHighPort());

        range = new PortRange("10-1000");
        Assert.assertEquals(10, range.getLowPort());
        Assert.assertEquals(1000, range.getHighPort());

        try {
            new PortRange("1a - 1000");
            Assert.fail("No number format exception.");
        } catch (NumberFormatException e) {
        }
        
        try {
            new PortRange("");
            Assert.fail("No illegal argument exception.");
        } catch (IllegalArgumentException e) {
        }
        
        try {
            new PortRange(null);
            Assert.fail("No illegal argument exception.");
        } catch (IllegalArgumentException e) {
        }
        
        range = PortManager.createPortRangeQuietly("1-2");
        Assert.assertNotNull(range);
        Assert.assertEquals(1, range.getLowPort());
        Assert.assertEquals(2, range.getHighPort());

        range = PortManager.createPortRangeQuietly(".1-2");
        Assert.assertNull(range);
    }

    /**
     * Tests port request instances.
     */
    @Test
    public void testPortRequest() {
        PortAssignmentRequest request = new PortAssignmentRequest("pip", "elt", 3, "local", "id1");
        Assert.assertTrue(request.doCheck());
        Assert.assertEquals("pip", request.getPipeline());
        Assert.assertEquals("elt", request.getElement());
        Assert.assertEquals(3, request.getTaskId());
        Assert.assertEquals("local", request.getHost());
        Assert.assertEquals("id1", request.getAssignmentId());
        
        request.setCheck(false);
        Assert.assertFalse(request.doCheck());
    }
    
    /**
     * Tests port assignment instances.
     */
    @Test
    public void testPortAssignment() {
        PortAssignment assng = new PortAssignment("host", 500, 4, null);
        Assert.assertEquals("host", assng.getHost());
        Assert.assertEquals(500, assng.getPort());
        Assert.assertEquals(4, assng.getTaskId());
        Assert.assertEquals(null, assng.getAssignmentId());
        Assert.assertTrue(assng.equalsAssigmentId(null));
        Assert.assertFalse(assng.equalsAssigmentId(""));
        
        assng = new PortAssignment("host", 500, 4, "id");
        Assert.assertEquals("host", assng.getHost());
        Assert.assertEquals(500, assng.getPort());
        Assert.assertEquals(4, assng.getTaskId());
        Assert.assertEquals("id", assng.getAssignmentId());
        Assert.assertFalse(assng.equalsAssigmentId(null));
        Assert.assertFalse(assng.equalsAssigmentId(""));
        Assert.assertTrue(assng.equalsAssigmentId("id"));
    }
    
    /**
     * Tests the port manager.
     */
    @Test
    public void testPortManager() {
        SignalException fail = null;
        Set<File> tmpFiles = TestHelper.trackTemp(null, false);
        LocalCluster cluster = new LocalCluster();

        String connectString = "localhost:" + TestHelper.LOCAL_ZOOKEEPER_PORT;
        CuratorFramework client = CuratorFrameworkFactory.builder().namespace(SignalMechanism.GLOBAL_NAMESPACE).
            connectString(connectString).retryPolicy(new RetryNTimes(5, 100)).build();
        client.start();

        PortManager mgr = new PortManager(client);
        try {
            mgr.clearAllPortAssignments();
            Assert.assertNull(mgr.getPortAssignment("pip", "switch", 5, null));
            Assert.assertNull(mgr.getPortAssignment("pip", "switch", 5, "id"));

            // create assignment and assert existence
            PortRange range = new PortRange(1000, 1001);
            PortAssignmentRequest req = new PortAssignmentRequest("pip", "element", 5, "localhost", "id");
            PortAssignment pa1 = assertPortAssignment(mgr, req, range, 1000);
            Assert.assertNull(mgr.getPortAssignment("pip", "element", 5, null));
            PortAssignment pa2 = mgr.getPortAssignment("pip", "element", 5, "id");
            Assert.assertEquals(pa1, pa2);
            // create second assignment
            req = new PortAssignmentRequest("pip", "element", 6, "localhost", null);
            PortAssignment pa3 = assertPortAssignment(mgr, req, range, 1001);
            Assert.assertNull(mgr.getPortAssignment("pip", "element", 6, "id"));
            PortAssignment pa4 = mgr.getPortAssignment("pip", "element", 6, null);
            Assert.assertEquals(pa3, pa4);
            // first is still there
            pa2 = mgr.getPortAssignment("pip", "element", 5, "id");
            Assert.assertEquals(pa1, pa2);
            // clear last assignment - initial one is still there
            mgr.clearPortAssignment("pip", "element", pa3);
            Assert.assertNull(mgr.getPortAssignment("pip", "element", 6, null));
            pa2 = mgr.getPortAssignment("pip", "element", 5, "id");
            Assert.assertEquals(pa1, pa2);
            // try with default range
            mgr = new PortManager(client, range);
            // register again - reuse port
            pa3 = assertPortAssignment(mgr, req, null, 1001); // use default range
            pa4 = mgr.getPortAssignment("pip", "element", 6, null);
            Assert.assertEquals(pa3, pa4);
            mgr.clearPortAssignment("pip", "element", pa3);
            Assert.assertNull(mgr.getPortAssignment("pip", "element", 6, null));
            listPorts(mgr, "After all assignments");

            // clear all assignments for pipeline
            mgr.clearPortAssignments("pip");
            Assert.assertNull(mgr.getPortAssignment("pip", "element", 5, "id"));
            listPorts(mgr, "After pipeline clear");

            mgr.clearAllPortAssignments();
            listPorts(mgr, "After all clear");
            mgr.close();
        } catch (SignalException e) {
            e.printStackTrace();
            fail = e;
        }
        
        client.close();
        cluster.shutdown();
        TestHelper.trackTemp(tmpFiles, true);
        fail = testClosed(client);
        if (null != fail) {
            Assert.fail(fail.getMessage());
        }
    }

    /**
     * Tests the port manager with multiple assignments / requests.
     */
    @Test
    public void testPortManagerMulti() {
        SignalException fail = null;
        Set<File> tmpFiles = TestHelper.trackTemp(null, false);
        LocalCluster cluster = new LocalCluster();

        String connectString = "localhost:" + TestHelper.LOCAL_ZOOKEEPER_PORT;
        CuratorFramework client = CuratorFrameworkFactory.builder().namespace(SignalMechanism.GLOBAL_NAMESPACE).
            connectString(connectString).retryPolicy(new RetryNTimes(5, 100)).build();
        client.start();

        PortManager mgr = new PortManager(client);
        try {
            // this is not really black-box - we assume that there are no gaps and ports are assigned linearly! 
            mgr.clearAllPortAssignments();
            PortRange range = new PortRange(1000, 1100);
            final int allocationCount = 15;
            System.out.println("Allocating for pip (" + allocationCount + "):");
            for (int i = 0; i < allocationCount; i++) {
                PortAssignmentRequest req = new PortAssignmentRequest("pip", "element_" + i, 5, "localhost", "id");
                PortAssignment p1 = assertPortAssignment(mgr, req, range, 1000 + i);
                PortAssignment p2 = mgr.getPortAssignment("pip", "element_" + i, 5, "id");
                Assert.assertEquals(p1, p2);
            }
            System.out.println("Allocating for pip1 (" + allocationCount + "):");
            for (int i = 0; i < allocationCount; i++) {
                PortAssignmentRequest req = new PortAssignmentRequest("pip1", "element_" + i, i, "localhost", "id");
                PortAssignment p1 = assertPortAssignment(mgr, req, range, 1000 + allocationCount + i);
                PortAssignment p2 = mgr.getPortAssignment("pip1", "element_" + i, i, "id");
                Assert.assertEquals(p1, p2);
            }
            listPorts(mgr, "After assignments for pip and pip1");
            System.out.println("Clearing pip:");
            mgr.clearPortAssignments("pip");
            Assert.assertNull(mgr.getPortAssignment("pip", "element", 5, "id"));
            System.out.println("Re-allocating for pip (" + allocationCount + "):");
            for (int i = 0; i < allocationCount; i++) {
                PortAssignmentRequest req = new PortAssignmentRequest("pip", "element_" + i, 5, "localhost", "id");
                PortAssignment p1 = assertPortAssignment(mgr, req, range, 1000 + i);
                PortAssignment p2 = mgr.getPortAssignment("pip", "element_" + i, 5, "id");
                Assert.assertEquals(p1, p2);
            }
            listPorts(mgr, "After all assignments");

            // clear all assignments for pipeline
            mgr.clearPortAssignments("pip");
            mgr.clearPortAssignments("pip1");
            Assert.assertNull(mgr.getPortAssignment("pip", "element_1", 5, "id"));
            Assert.assertNull(mgr.getPortAssignment("pip1", "element_" + allocationCount, 5, "id"));
            listPorts(mgr, "After pip and pip1 clear");

            mgr.close();
        } catch (SignalException e) {
            e.printStackTrace();
            fail = e;
        }
        
        client.close();
        cluster.shutdown();
        TestHelper.trackTemp(tmpFiles, true);
        fail = testClosed(client);
        if (null != fail) {
            Assert.fail(fail.getMessage());
        }
    }

    
    /**
     * Lists the ports in the given port manager.
     * 
     * @param mgr the manager
     * @param text gives some additional information
     * @throws SignalException in case that reading data fails
     */
    private void listPorts(PortManager mgr, String text) throws SignalException {
        System.out.println("> ---- " + text + " ----");
        mgr.listPorts(System.out);
        System.out.println("< ---- " + text + " ----");
    }
    
    /**
     * Tests a closed client connection.
     * 
     * @param client the closed connection
     * @return exception if occurred
     */
    private SignalException testClosed(CuratorFramework client) {
        SignalException fail = null;
        try {
            PortRange range = new PortRange(1000, 1001);
            PortManager mgr = new PortManager(client, range);
            mgr.clearAllPortAssignments();
            mgr.clearPortAssignments("pip");
            mgr.getPortAssignment("pip", "element", 5, "id");
            PortAssignmentRequest req = new PortAssignmentRequest("pip", "element", 6, "localhost", null);
            mgr.registerPortAssignment(req);
            mgr.close();
        } catch (SignalException e) {
            e.printStackTrace();
            fail = e;
        }
        return fail;
    }
    
    /**
     * Asserts a port assignment while registration.
     * 
     * @param mgr the port manager
     * @param req the assignment request
     * @param range the allowed port range
     * @param expectedPort the expected port
     * @return the created port assignment (may be <b>null</b>)
     * @throws SignalException shall not occur
     */
    private PortAssignment assertPortAssignment(PortManager mgr, PortAssignmentRequest req, PortRange range, 
        int expectedPort) throws SignalException {
        PortAssignment assng;
        if (null == range) {
            assng = mgr.registerPortAssignment(req);
        } else {
            assng = mgr.registerPortAssignment(req, range);
        }
        Assert.assertNotNull(assng);
        Assert.assertEquals(req.getTaskId(), assng.getTaskId());
        Assert.assertEquals(expectedPort, assng.getPort());
        Assert.assertEquals(req.getAssignmentId(), assng.getAssignmentId());
        Assert.assertEquals(req.getHost(), assng.getHost());
        return assng;
    }
    
    /**
     * Implements a test and assertion port assignment watcher.
     * 
     * @author Holger Eichelberger
     */
    private static class PortAssignmentWatcher implements IPortAssignmentWatcher {

        private PortAssignmentRequest request;
        private PortAssignment assignment;
        
        @Override
        public void notifyPortAssigned(PortAssignmentRequest request, PortAssignment assignment) {
            this.request = request;
            this.assignment = assignment;
        }
        
        /**
         * Asserts a received assignment.
         * 
         * @param request the expected request
         * @param assignment the expected assignment
         */
        private void assertAssignment(PortAssignmentRequest request, PortAssignment assignment) {
            // no equals defined, no identity due to serialization
            if (null != request) {
                Assert.assertNotNull(this.request);
                Assert.assertEquals(request.getPipeline(), this.request.getPipeline());
                Assert.assertEquals(request.getElement(), this.request.getElement());
                Assert.assertEquals(request.getHost(), this.request.getHost());
                Assert.assertEquals(request.getTaskId(), this.request.getTaskId());
            } else {
                Assert.assertNull(this.request);
            }
            Assert.assertEquals(assignment, this.assignment);
        }

        /**
         * Clears the watcher.
         */
        private void clear() {
            request = null;
            assignment = null;
        }
        
    }
    
    /**
     * Tests the watcher.
     */
    @Test
    public void testWatcher() {
        SignalException fail = null;
        Set<File> tmpFiles = TestHelper.trackTemp(null, false);
        LocalCluster cluster = new LocalCluster();

        String connectString = "localhost:" + TestHelper.LOCAL_ZOOKEEPER_PORT;
        CuratorFramework client = CuratorFrameworkFactory.builder().namespace(SignalMechanism.GLOBAL_NAMESPACE).
            connectString(connectString).retryPolicy(new RetryNTimes(5, 100)).build();
        client.start();

        PortRange range = new PortRange(1000, 1001);
        PortManager mgr = new PortManager(client, range);
        try {
            mgr.clearAllPortAssignments();
            PortAssignmentWatcher watcher = new PortAssignmentWatcher();
            mgr.setPortAssigmentWatcher("pip", "element", 5, "id", watcher);
            PortAssignmentRequest req = new PortAssignmentRequest("pip", "element", 5, "localhost", "id");
            PortAssignment pa = assertPortAssignment(mgr, req, range, 1000);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            watcher.assertAssignment(req, pa);
            watcher.clear();
            
            req = new PortAssignmentRequest("pip", "element", 6, "localhost", "id");
            pa = assertPortAssignment(mgr, req, range, 1001);
            mgr.setPortAssigmentWatcher("pip", "element", 6, "id", watcher);
            watcher.assertAssignment(req, pa);
            
            mgr.close();
        } catch (SignalException e) {
            e.printStackTrace();
            fail = e;
        }

        client.close();
        cluster.shutdown();
        TestHelper.trackTemp(tmpFiles, true);
        fail = testClosed(client);
        if (null != fail) {
            Assert.fail(fail.getMessage());
        }
    }
    
    /**
     * Tests a problem case from a pipeline.
     */
    @Test
    public void testPip() {
        SignalException fail = null;
        Set<File> tmpFiles = TestHelper.trackTemp(null, false);
        LocalCluster cluster = new LocalCluster();

        String connectString = "localhost:" + TestHelper.LOCAL_ZOOKEEPER_PORT;
        CuratorFramework client = CuratorFrameworkFactory.builder().namespace(SignalMechanism.GLOBAL_NAMESPACE).
            connectString(connectString).retryPolicy(new RetryNTimes(5, 100)).build();
        client.start();

        PortManager mgr = new PortManager(client);
        try {
            // this is not really black-box - we assume that there are no gaps and ports are assigned linearly! 
            mgr.clearAllPortAssignments();
            PortRange range = new PortRange(1000, 1100);
            PortAssignmentRequest req = new PortAssignmentRequest("pip", "element", 0, "localhost", null);
            PortAssignment p1 = assertPortAssignment(mgr, req, range, 1000);
            PortAssignment p2 = mgr.getPortAssignment("pip", "element", 0, null);
            Assert.assertEquals(p1, p2);
            mgr.close();
        } catch (SignalException e) {
            e.printStackTrace();
            fail = e;
        }
        
        client.close();
        cluster.shutdown();
        TestHelper.trackTemp(tmpFiles, true);
        fail = testClosed(client);
        if (null != fail) {
            Assert.fail(fail.getMessage());
        }
    }

}
