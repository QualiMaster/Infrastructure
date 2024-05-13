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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.Assert;

import backtype.storm.daemon.common.Assignment;
import clojure.lang.IPersistentMap;
import clojure.lang.MapEntry;
import clojure.lang.PersistentArrayMap;
import tests.eu.qualimaster.storm.Naming;
import eu.qualimaster.coordination.TaskAssignment;
import eu.qualimaster.coordination.ZkUtils;

/**
 * Tests the accessible methods of {@link TaskAssignment}.
 * 
 * @author Holger Eichelberger
 */
public class TaskAssignmentTest {
    
    private static final String HOST_ID = "#abba";
    
    /**
     * A test for the creation of a {@link TaskAssignment}.
     */
    @Test
    public void testCreation() {
        final String component = "process";
        final int port = 1234;
        final int taskStart = 1;
        final int taskEnd = 1;
        final int time = 1223;
        
        TaskAssignment assng = new TaskAssignment(taskStart, taskEnd, HOST_ID, port, component);
        Assert.assertEquals(0, assng.getStartTime());
        Assert.assertEquals(taskStart, assng.getTaskStart());
        Assert.assertEquals(taskEnd, assng.getTaskEnd());
        Assert.assertEquals(HOST_ID, assng.getHostId());
        Assert.assertEquals(port, assng.getPort());
        Assert.assertEquals(component, assng.getComponent());
        Assert.assertTrue(assng.isValid());
        Assert.assertTrue(assng.isActive());
        Assert.assertTrue(!assng.isDisabled());
        assng.setStartTime(time);
        Assert.assertEquals(time, assng.getStartTime());
        assng.disable();
        Assert.assertTrue(!assng.isActive());
        Assert.assertTrue(assng.isDisabled());
        
        Assert.assertEquals(toString(taskStart, taskEnd), assng.getExecutorIdString());
        Assert.assertEquals(toString(HOST_ID, port), assng.getHostPortString());

        assng = new TaskAssignment(taskStart, taskStart, HOST_ID, port, component);
        Assert.assertEquals(taskStart, assng.getTaskStart());
        Assert.assertEquals(taskStart, assng.getTaskEnd());
        Assert.assertTrue(assng.isValid());

        assng = new TaskAssignment(2, 1, HOST_ID, port, component);
        Assert.assertTrue(!assng.isValid());

        assng = new TaskAssignment(2, 1, null, port, component);
        Assert.assertTrue(!assng.isValid());

        assng = new TaskAssignment(2, 1, HOST_ID, -1, component);
        Assert.assertTrue(!assng.isValid());

        assng = new TaskAssignment(2, 1, HOST_ID, -1, null);
        Assert.assertTrue(!assng.isValid());
    }
    
    /**
     * Turns arbitrary objects into a Storm list string.
     * 
     * @param objects the objects to be turned into a Strom list string
     * @return the Storm list string
     */
    private static String toString(Object... objects) {
        StringBuilder result = new StringBuilder("[");
        if (null != objects) {
            for (int o = 0; o < objects.length; o++) {
                if (o > 0) {
                    result.append(" ");
                }
                Object ob = objects[o];
                if (ob instanceof String) {
                    result.append("\"");
                }
                result.append(ob);
                if (ob instanceof String) {
                    result.append("\"");
                }
            }
        }
        result.append("]");
        return result.toString();
    }

    /**
     * Tests turning a task assignment into as Storm assignment and back again.
     */
    @Test
    public void testToFromStorm() {
        final String host = "localhost";
        final String path = "path";
        Map<String, String> nodeHostMap = new HashMap<String, String>();
        nodeHostMap.put(HOST_ID, host);
        IPersistentMap execMapping = PersistentArrayMap.create(nodeHostMap); 
        Map<String, List<TaskAssignment>> assignments = createTestAssignments();
        Map<String, String> assng = new HashMap<String, String>();
        Map<String, String> times = new HashMap<String, String>();
        Map<Integer, String> taskComponents = toTaskComponents(assignments);
        for (List<TaskAssignment> assigns : assignments.values()) {
            for (TaskAssignment a : assigns) {
                assng.put(a.getExecutorIdString(), a.getHostPortString());
                times.put(a.getExecutorIdString(), String.valueOf(a.getStartTime()));
            }
        }
        Assignment original = ZkUtils.createAssignment(path, execMapping, null, null, null);
        List<String> workerSeq = new ArrayList<String>();
        Assignment result = TaskAssignment.createTaskAssignments(original, null, assignments, workerSeq);
        Assert.assertEquals(path, result.master_code_dir);
        Assert.assertEquals(execMapping, result.node__GT_host);
        Assert.assertTrue(result.executor__GT_node_PLUS_port instanceof PersistentArrayMap);
        Assert.assertTrue(result.executor__GT_start_time_secs instanceof PersistentArrayMap);
        if (ZkUtils.isQmStormVersion()) {
            Assert.assertNotNull(ZkUtils.getWorkerDependencies(result));
        }
        PersistentArrayMap am = (PersistentArrayMap) result.executor__GT_node_PLUS_port;
        Assert.assertEquals(assng.size(), am.size());
        for (Object e : am.entrySet()) {
            Assert.assertTrue(e instanceof MapEntry);
            MapEntry me = (MapEntry) e;
            String key = me.getKey().toString();
            String expectedVal = assng.remove(key);
            Assert.assertNotNull(expectedVal);
            Assert.assertEquals(expectedVal, me.getValue().toString());
        }
        Assert.assertTrue(assng.isEmpty());
        am = (PersistentArrayMap) result.executor__GT_start_time_secs;
        Assert.assertEquals(times.size(), am.size());
        for (Object e : ((PersistentArrayMap) result.executor__GT_start_time_secs).entrySet()) {
            Assert.assertTrue(e instanceof MapEntry);
            MapEntry me = (MapEntry) e;
            String key = me.getKey().toString();
            String expectedVal = times.remove(key);
            Assert.assertNotNull(expectedVal);
            Assert.assertEquals(expectedVal, me.getValue().toString());
        }
        Assert.assertTrue(times.isEmpty());
        Map<String, List<TaskAssignment>> rAssignments = TaskAssignment.readTaskAssignments(result, taskComponents);
        Assert.assertEquals(assignments.size(), rAssignments.size());
        for (Map.Entry<String, List<TaskAssignment>> entry : assignments.entrySet()) {
            List<TaskAssignment> expected = entry.getValue();
            List<TaskAssignment> actual = rAssignments.get(entry.getKey());
            Assert.assertNotNull(actual);
            Assert.assertEquals(expected.size(), actual.size());
            Map<String, TaskAssignment> expectedMap = toMap(expected);
            Map<String, TaskAssignment> actualMap = toMap(actual);
            for (Map.Entry<String, TaskAssignment> eEntry : expectedMap.entrySet()) {
                TaskAssignment expectedAssng = eEntry.getValue();
                TaskAssignment actualAssng = actualMap.get(eEntry.getKey());
                Assert.assertNotNull(actualAssng);
                Assert.assertEquals(expectedAssng.getStartTime(), actualAssng.getStartTime());
                Assert.assertEquals(expectedAssng.getTaskStart(), actualAssng.getTaskStart());
                Assert.assertEquals(expectedAssng.getTaskEnd(), actualAssng.getTaskEnd());
                Assert.assertEquals(expectedAssng.getHostId(), actualAssng.getHostId());
                Assert.assertEquals(expectedAssng.getPort(), actualAssng.getPort());
                Assert.assertEquals(expectedAssng.getComponent(), actualAssng.getComponent());
            }
        }
    }
    
    /**
     * Turns a list of task assignments into a map using {@link TaskAssignment#getExecutorIdString()} as key.
     * 
     * @param assignments the assignments to be turned into the map 
     * @return the resulting map
     */
    private static Map<String, TaskAssignment> toMap(List<TaskAssignment> assignments) {
        Map<String, TaskAssignment> result = new HashMap<String, TaskAssignment>();
        for (TaskAssignment assng : assignments) {
            result.put(assng.getExecutorIdString(), assng);
        }
        return result;
    }

    /**
     * Turns task assignments into their (summarizing) task components.
     * 
     * @param assignments the assignments
     * @return the taskid-component mapping
     */
    static Map<Integer, String> toTaskComponents(Map<String, List<TaskAssignment>> assignments) {
        Map<Integer, String> taskComponents = new HashMap<Integer, String>();
        for (List<TaskAssignment> assigns : assignments.values()) {
            for (TaskAssignment a : assigns) {
                for (int t = a.getTaskStart(); t <= a.getTaskEnd(); t++) {
                    taskComponents.put(t, a.getComponent());
                }
            }
        }
        return taskComponents;
    }

    
    /**
     * Creates some assignments.
     * 
     * @return the assignments
     */
    static Map<String, List<TaskAssignment>> createTestAssignments() {
        final String hostId = "#abba";
        final int port = 1234;

        Map<String, List<TaskAssignment>> assignments = new HashMap<String, List<TaskAssignment>>();

        TaskAssignment assng = new TaskAssignment(1, 2, hostId, port, Naming.NODE_SOURCE);
        assng.setStartTime(12);
        List<TaskAssignment> assngs = new ArrayList<TaskAssignment>();
        assngs.add(assng);
        assignments.put(assng.getComponent(), assngs);

        assng = new TaskAssignment(3, 3, hostId, port, Naming.NODE_PROCESS);
        assng.setStartTime(12);
        assngs = new ArrayList<TaskAssignment>();
        assngs.add(assng);
        assng = new TaskAssignment(4, 5, hostId, port, Naming.NODE_PROCESS);
        assng.setStartTime(12);
        assngs.add(assng);
        assng = new TaskAssignment(6, 7, hostId, port, Naming.NODE_PROCESS);
        assng.setStartTime(12);
        assngs.add(assng);
        assignments.put(assng.getComponent(), assngs);

        assng = new TaskAssignment(8, 8, hostId, port, Naming.NODE_SINK);
        assng.setStartTime(12);
        assngs = new ArrayList<TaskAssignment>();
        assngs.add(assng);
        assignments.put(assng.getComponent(), assngs);

        return assignments;
    }

}
