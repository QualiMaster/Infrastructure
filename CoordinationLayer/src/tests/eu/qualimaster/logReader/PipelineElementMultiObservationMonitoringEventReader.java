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

import java.util.Map;

import eu.qualimaster.monitoring.events.ComponentKey;
import eu.qualimaster.monitoring.events.PipelineElementMultiObservationMonitoringEvent;
import eu.qualimaster.observables.IObservable;

/**
 * Reads {@link PipelineElementMultiObservationMonitoringEventReader}.
 * 
 * @author Holger Eichelberger
 */
public class PipelineElementMultiObservationMonitoringEventReader 
    extends EventReader<PipelineElementMultiObservationMonitoringEvent> {

    /**
     * Creates the reader.
     */
    public PipelineElementMultiObservationMonitoringEventReader() {
        super(PipelineElementMultiObservationMonitoringEvent.class);
    }
    
    @Override
    public PipelineElementMultiObservationMonitoringEvent parseEvent(String line, LogReader reader) {
        PipelineElementMultiObservationMonitoringEvent result = null;
        EventLineParser parser = new EventLineParser(line, reader.getErr());
        int startLineLength;
        Map<IObservable, Double> observations = null;
        String pipelineElement = null;
        ComponentKey key;
        String pipeline = null;
        do {
            startLineLength = line.length();
            observations = parser.parseObservations("observations");
            pipelineElement = parser.parseString("pipelineElement", pipelineElement);
            key = parser.parseComponentKey("key");
            pipeline = parser.parseString("pipeline", pipeline);
        } while (!parser.isEndOfLine(startLineLength));
        if (null != observations && null != pipelineElement && null != pipeline) { // key may be null
            result = new PipelineElementMultiObservationMonitoringEvent(pipeline, pipelineElement, key, observations);
        }
        return result;
    }

}
