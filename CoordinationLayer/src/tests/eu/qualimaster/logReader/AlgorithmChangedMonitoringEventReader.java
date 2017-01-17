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
package tests.eu.qualimaster.logReader;

import java.util.HashSet;
import java.util.Set;

import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;

/**
 * Implements an algorithm changed monitoring event reader.
 * 
 * @author Holger Eichelberger
 */
public class AlgorithmChangedMonitoringEventReader extends EventReader<AlgorithmChangedMonitoringEvent> {

    private static final Set<String> STOP = new HashSet<String>();
    static {
        STOP.add("algorithm");
        STOP.add("pipelineElement");
        STOP.add("key");
        STOP.add("pipeline");
        STOP.add("causeMsgId");
    }
    
    /**
     * Creates the reader.
     */
    protected AlgorithmChangedMonitoringEventReader() {
        super(AlgorithmChangedMonitoringEvent.class);
    }

    //algorithm: SimpleStateTransferSW pipelineElement: processor key: null pipeline: SwitchPip
    
    @Override
    public AlgorithmChangedMonitoringEvent parseEvent(String line, LogReader reader) {
        AlgorithmChangedMonitoringEvent result = null;
        EventLineParser parser = new EventLineParser(line, reader.getErr());
        int startLineLength;
        String algorithm = null;
        String pipelineElement = null;
        String pipeline = null;
        int i = 0;
        do {
            startLineLength = line.length();
            algorithm = parser.parseString("algorithm", algorithm, STOP);
            pipelineElement = parser.parseString("pipelineElement", pipelineElement, STOP);
            parser.parseComponentKey("key"); // key may be null
            pipeline = parser.parseString("pipeline", pipeline, STOP);
            parser.parseString("causeMsgId", null); // ignore
            i++;
        } while (!parser.isEndOfLine(startLineLength) && i < 50);
        if (null != algorithm && null != pipelineElement && null != pipeline) { 
            result = new AlgorithmChangedMonitoringEvent(pipeline, pipelineElement, algorithm);
        }
        return result;
    }

}
