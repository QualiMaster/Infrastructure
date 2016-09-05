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

import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.monitoring.topology.PipelineTopology.Processor;
import eu.qualimaster.monitoring.topology.PipelineTopology.Stream;

/**
 * Just a refined processor for building up the internal topology.
 * 
 * @author Holger Eichelberger
 */
public class TestProcessor extends Processor {

    /**
     * Creates a processor if detailed settings do not care.
     * 
     * @param name the name
     */
    public TestProcessor(String name) {
        super(name, 1, null);
    }
    
    /**
     * Creates a processor.
     * 
     * @param name the name
     * @param parallelization the parallelization degree
     * @param tasks the tasks
     */
    public TestProcessor(String name, int parallelization, int[] tasks) {
        super(name, parallelization, tasks);
    }

    /**
     * Defines the inputs.
     * 
     * @param inputs the inputs
     */
    public void setInputs(Stream... inputs) {
        List<Stream> tmp = new ArrayList<Stream>();
        for (Stream s : inputs) {
            tmp.add(s);
        }
        super.setInputs(tmp);
    }

    /**
     * Defines the outputs.
     * 
     * @param outputs the outputs
     */
    public void setOutputs(Stream... outputs) {
        List<Stream> tmp = new ArrayList<Stream>();
        for (Stream s : outputs) {
            tmp.add(s);
        }
        super.setOutputs(tmp);
    }

}

