/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
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
package tests.eu.qualimaster.coordination;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import backtype.storm.daemon.common.Assignment;
import tests.eu.qualimaster.storm.Naming;
import eu.qualimaster.coordination.HostPort;
import eu.qualimaster.coordination.ParallelismChangeRequest;
import eu.qualimaster.coordination.StormUtils;
import eu.qualimaster.coordination.StormUtils.ITopologySupport;
import eu.qualimaster.coordination.TaskAssignment;
import eu.qualimaster.coordination.ZkUtils;

/**
 * Tests the {@link StormUtils} as far as possible without a running storm instance. Other methods shall be tested
 * from within a local Storm test environment.
 *  
 * @author Holger Eichelberger
 */
public class StormUtilsTests {

    private static final String LOCALHOST = "localhost";
    
    /**
     * A test implementation of the topology support interface.
     * 
     * @author Holger Eichelberger
     */
    private static class TestTopologySupport implements ITopologySupport {

        @Override
        public HostPort getHostAssignment(TaskAssignment assignment, ParallelismChangeRequest request) {
            HostPort result;
            if (null == request.getHost()) {
                result = new HostPort(assignment.getHostId(), assignment.getPort());    
            } else {
                result = new HostPort(getHostId(request.getHost()), assignment.getPort());
            }
            return result;
        }

        @Override
        public Map<String, String> getHostIdMapping() {
            Map<String, String> result = new HashMap<String, String>();
            result.put(getHostId(LOCALHOST), LOCALHOST);
            return result;
        }

        @Override
        public int getTimestamp() {
            return -1;
        }
        
    }
    
    /**
     * Just faking a simple host id.
     * 
     * @param host the host name
     * @return the host id
     */
    private static final String getHostId(String host) {
        return "#" + host;
    }
    
    /**
     * Tests changing the parallelism.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testChangeParallelism() throws IOException {
        Map<String, List<TaskAssignment>> assignments = TaskAssignmentTest.createTestAssignments();
        assertContinuity(assignments);
        
        Map<String, ParallelismChangeRequest> changes = new HashMap<String, ParallelismChangeRequest>();
        Map<String, ParallelismChangeRequest> expectedLeftOver = new HashMap<String, ParallelismChangeRequest>();

        changes.clear(); // no changes - no modification
        expectedLeftOver.clear(); // no changes - no expected left over
        assertChangeParallelism(assignments, changes, false, changes);
        
        changes.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(1));
        expectedLeftOver.clear(); // can be fulfilled
        assertChangeParallelism(assignments, changes, true, expectedLeftOver);
        
        changes.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(2));
        expectedLeftOver.clear(); // can be fulfilled
        assertChangeParallelism(assignments, changes, true, expectedLeftOver);

        changes.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(5));
        expectedLeftOver.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(3)); // 2 is maximum
        assertChangeParallelism(assignments, changes, true, expectedLeftOver);

        changes.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(-1));
        expectedLeftOver.clear();
        assertChangeParallelism(assignments, changes, true, expectedLeftOver);

        changes.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(-2));
        expectedLeftOver.clear();
        assertChangeParallelism(assignments, changes, true, expectedLeftOver);

        changes.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(-5));
        expectedLeftOver.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(-3)); // 2 is maximum
        assertChangeParallelism(assignments, changes, true, expectedLeftOver);
        
        changes.put(Naming.NODE_PROCESS, new ParallelismChangeRequest(1, LOCALHOST));
        expectedLeftOver.clear(); // can be fulfilled
        assertChangeParallelism(assignments, changes, true, expectedLeftOver);
    }
    
    /**
     * Unmatching tests changing the parallelism.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testUnknown() throws IOException {
        Map<String, List<TaskAssignment>> assignments = TaskAssignmentTest.createTestAssignments();
        assertContinuity(assignments);
        
        Map<String, ParallelismChangeRequest> changes = new HashMap<String, ParallelismChangeRequest>();
        Map<String, ParallelismChangeRequest> expectedLeftOver = new HashMap<String, ParallelismChangeRequest>();
        
        final String executor = "unknown";
        changes.put(executor, new ParallelismChangeRequest(1));
        expectedLeftOver.put(executor, new ParallelismChangeRequest(1));
        assertChangeParallelism(assignments, changes, false, expectedLeftOver);
    }

    /**
     * Asserts the result of executing the change parallelism functionality.
     * 
     * @param assignments the initial assignments (will not be modified)
     * @param changes the desired changes (will not be modified)
     * @param expectChanges whether changes to the initial assignments are expected
     * @param expectedLeftOver those changes that cannot be fulfilled (will not be modified)
     * @throws IOException shall not occur
     */
    private static void assertChangeParallelism(Map<String, List<TaskAssignment>> assignments, 
        Map<String, ParallelismChangeRequest> changes, boolean expectChanges, 
        Map<String, ParallelismChangeRequest> expectedLeftOver) throws IOException {
        Map<String, ParallelismChangeRequest> tmpChanges = deepCopy(changes);
        Map<Integer, String> taskComponents = TaskAssignmentTest.toTaskComponents(assignments);
        Assignment assng = ZkUtils.createAssignment("", null, null, null, null); // content is irrelevant
        assng = TaskAssignment.createTaskAssignments(assng, null, assignments, null);
        ITopologySupport tdata = new TestTopologySupport();
        Assignment rAssng = StormUtils.changeParallelism(assng, tmpChanges, taskComponents, tdata);
        if (expectChanges) {
            Assert.assertNotNull(rAssng); // there shall be changes
            if (ZkUtils.isQmStormVersion()) {
                Assert.assertNotNull(ZkUtils.getWorkerDependencies(rAssng));
            }
            Map<String, List<TaskAssignment>> rAssignments = TaskAssignment.readTaskAssignments(rAssng, taskComponents);
            Assert.assertNotNull(rAssignments); // there shall be changes
            assertContinuity(rAssignments);
            for (Map.Entry<String, List<TaskAssignment>> entry : assignments.entrySet()) {
                String component = entry.getKey();
                int executorsBefore = sizeSafe(entry.getValue());
                int desiredChange = getSafe(component, changes);
                int leftOver = getSafe(component, tmpChanges);
                int executorsAfter = sizeSafe(rAssignments.get(component));
                Assert.assertEquals("before " + executorsBefore + " change " + desiredChange + " leftOver " + leftOver 
                    + " -!-> " + executorsAfter, executorsBefore + desiredChange - leftOver, executorsAfter);
            }
            // check hosts
            for (Map.Entry<String, ParallelismChangeRequest> entry : changes.entrySet()) {
                String component = entry.getKey();
                ParallelismChangeRequest request = entry.getValue();
                if (null != request.getHost()) {
                    ParallelismChangeRequest response = tmpChanges.get(component);
                    if (null != response) {
                        // shall not be modified
                        Assert.assertEquals(request.getHost(), response.getHost());
                        Assert.assertEquals(request.otherHostThenAssignment(), response.otherHostThenAssignment());
                    }
                    if (null == response || request.getExecutorDiff() != response.getExecutorDiff()) {
                        String reqHostId = getHostId(request.getHost());
                        List<TaskAssignment> a = rAssignments.get(component);
                        Assert.assertNotNull(a);
                        int desiredChange = getSafe(component, changes);
                        int leftOver = getSafe(component, tmpChanges);
                        int count = 0; // not sure what to test in the future
                        for (int i = 0; i < a.size(); i++) {
                            if (a.get(i).getHostId().equals(reqHostId)) {
                                count++;
                            }
                        }
                        Assert.assertTrue(count == desiredChange - leftOver + 1); // +1 for split
                    }
                }
            }
        } else {
            Assert.assertNull(rAssng);
            Assert.assertEquals(changes, tmpChanges);
        }
        if (null != expectedLeftOver) {
            Assert.assertEquals(expectedLeftOver, tmpChanges);
        }
    }
    
    /**
     * Performs a deep copy of <code>orig</code>.
     * 
     * @param orig the original to be copied
     * @return the copy
     */
    private static Map<String, ParallelismChangeRequest> deepCopy(Map<String, ParallelismChangeRequest> orig) {
        Map<String, ParallelismChangeRequest> result = new HashMap<String, ParallelismChangeRequest>();    
        for (Map.Entry<String, ParallelismChangeRequest> entry : orig.entrySet()) {
            result.put(entry.getKey(), new ParallelismChangeRequest(entry.getValue()));
        }
        return result;
    }
    
    /**
     * Returns the executor diff and 0 if the entry does not exist.
     * 
     * @param key the key to look for
     * @param map the map to look within
     * @return the value
     */
    private static int getSafe(String key, Map<String, ParallelismChangeRequest> map) {
        ParallelismChangeRequest tmp = map.get(key);
        return null == tmp ? 0 : tmp.getExecutorDiff();
    }

    /**
     * Returns the size of a list and 0 if the list is <b>null</b>.
     * 
     * @param list the list to return the size for
     * @return the list size
     */
    private static int sizeSafe(List<?> list) {
        return null == list ? 0 : list.size();
    }
    
    /**
     * Asserts continuity in the assignment, i.e., all task ids are used starting from 1 to the maximum task id.
     * 
     * @param assignments the assignments to analyze
     */
    private static void assertContinuity(Map<String, List<TaskAssignment>> assignments) {
        Set<Integer> taskIDs = new HashSet<Integer>();
        for (List<TaskAssignment> list : assignments.values()) {
            for (TaskAssignment assng : list) {
                for (int t = assng.getTaskStart(); t <= assng.getTaskEnd(); t++) {
                    Assert.assertTrue(!taskIDs.contains(t));
                    taskIDs.add(t);
                }
            }
        }
        
        int task = 1;
        while (taskIDs.size() > 0) {
            Assert.assertTrue("taskid " + task + " missing", taskIDs.remove(task++));
        }
    }


}
