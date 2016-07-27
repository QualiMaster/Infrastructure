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
package tests.eu.qualimaster.monitoring.genTopo;

import backtype.storm.Config;
import backtype.storm.topology.TopologyBuilder;
import eu.qualimaster.base.algorithm.ITopologyCreate;
import eu.qualimaster.base.algorithm.SubTopologyOutput;
import eu.qualimaster.base.pipeline.RecordingTopologyBuilder;

/**
 * Implements a test subtopology.
 * 
 * @author Holger Eichelberger
 */
public class TopoSoftwareCorrelationFinancial extends AbstractHyTopology implements ITopologyCreate {
    // subclasses AbstractHyTopology just for the names...

    private String namespace;

    /**
     * Creates a correlation subtopology.
     * 
     * @param namespace the namespace of the pipeline
     */
    public TopoSoftwareCorrelationFinancial(String namespace) {
        this.namespace = namespace;
    }
    
    @Override
    public SubTopologyOutput createSubTopology(TopologyBuilder builder, Config config, String prefix, String input,
                    String streamId) {
        builder.setBolt(getHyMapperName(), 
            new SubTopologyFamilyElement0FamilyElement(getHyMapperName(), namespace, false, false), 1)
            .setNumTasks(1).shuffleGrouping(input);
        builder.setBolt(getHyProcessorName(), 
            new SubTopologyFamilyElement1FamilyElement(getHyProcessorName(), namespace, false, false), 1)
            .setNumTasks(3).shuffleGrouping(getHyMapperName());
        return new SubTopologyOutput(getHyProcessorName(), streamId, 1, 1);
    }

    @Override
    public void createTopology(Config config, RecordingTopologyBuilder builder) {
    }

    @Override
    public String getName() {
        return null; // not standalone
    }

    @Override
    protected String getAlgorithmName() {
        return null; // not standalone
    }

    @Override
    protected boolean isThrift() {
        return true;
    }

    @Override
    public String getMappingFileName() {
        return null;
    }

}
