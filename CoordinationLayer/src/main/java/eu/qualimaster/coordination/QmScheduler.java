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
package eu.qualimaster.coordination;


import java.util.HashMap;
import java.util.Map;

import backtype.storm.scheduler.Cluster;
import backtype.storm.scheduler.EvenScheduler;
import backtype.storm.scheduler.SchedulerAssignment;
import backtype.storm.scheduler.Topologies;
import backtype.storm.scheduler.TopologyDetails;

/**
 * A test scheduler.
 * 
 * @author Holger Eichelberger
 */
public class QmScheduler extends EvenScheduler {
    
    // yaml: nimbus.scheduler: "com.mycompany.SchedulerA"
    
    /**
     * Creates the scheduler.
     */
    public QmScheduler() {
        System.out.println("QMSCHEDULER CREATED");
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public void prepare(Map conf) {
        System.out.println("QMSCHEDULER " + conf);
        super.prepare(conf);
    }
    
    @Override
    public void schedule(Topologies topologies, Cluster cluster) {
        System.out.println("QMSCHEDULER schedule");
        Map<String, TopologyDetails> needScheduling = new HashMap<String, TopologyDetails>();
        for (TopologyDetails topo : topologies.getTopologies()) {
            System.out.println(topo.getId() + " " + topo.getName());
            SchedulerAssignment assignment = cluster.getAssignmentById(topo.getId());
            if (cluster.needsScheduling(topo)) {
                if (null == assignment) { // just do the initial assignment
                    needScheduling.put(topo.getId(), topo);
                    System.out.println("NO ASSIGNMENT");
                    //System.out.println("Scheduling " + topo.getId() + " " + topo.getName());
                } else {
                    System.out.println("ASSNG " + assignment.getExecutorToSlot());
                    System.out.println("ASSNG " + assignment.getExecutors());
                    System.out.println("ASSNG " + assignment.getSlots());
                }
            } else {
                System.out.println("ASSNG " + assignment.getExecutorToSlot());
                System.out.println("ASSNG " + assignment.getExecutors());
                System.out.println("ASSNG " + assignment.getSlots());
            }
        }
        if (!needScheduling.isEmpty()) {
            Topologies topo = new Topologies(needScheduling);
            super.schedule(topo, cluster);
        }
        //super.schedule(topologies, cluster);
    }

}
