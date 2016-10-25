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
package eu.qualimaster.adaptation.events;

import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.common.QMInternal;

/**
 * Allows the source volume prediction to alert the adaptation layer about significantly changing
 * volumes.
 * 
 * @author Holger Eichelberger
 * @author Andrea Ceroni
 */
public class SourceVolumeAdaptationEvent extends AdaptationEvent implements IPipelineAdaptationEvent {

    private static final long serialVersionUID = -1484370408104441395L;
    private String pipeline;
    private String source;
    private Map<String, Double> findings = new HashMap<String, Double>();
    private Map<String, Double> normalizedFindings = new HashMap<String, Double>();

    /**
     * Creates a source volume adaptation event.
     * 
     * @param pipeline the pipeline name
     * @param source the source name
     * @param key the key/term causing the event
     * @param deviation the deviation in volume indicating a problem
     */
    @QMInternal
    public SourceVolumeAdaptationEvent(String pipeline, String source, String key, double deviation) {
        this(pipeline, source, createSingleFinding(key, deviation));
    }

    /**
     * Creates a source volume adaptation event.
     * 
     * @param pipeline the pipeline name
     * @param source the source name
     * @param findings the findings
     * @throws IllegalArgumentException if <code>findings</code> is <b>null</b> or empty
     */
    @QMInternal
    public SourceVolumeAdaptationEvent(String pipeline, String source, Map<String, Double> findings) {
        if (null == findings || findings.isEmpty()) {
            throw new IllegalArgumentException("no findings");
        }
        this.pipeline = pipeline;
        this.source = source;
        this.findings = findings;
    }
    
    /**
     * Creates a source volume adaptation event.
     * 
     * @param pipeline the pipeline name
     * @param source the source name
     * @param findings the findings
     * @param normFindings the normalized findings
     * @throws IllegalArgumentException if <code>findings</code> is <b>null</b> or empty
     */
    @QMInternal
    public SourceVolumeAdaptationEvent(String pipeline, String source, Map<String, Double> findings,
        Map<String, Double> normFindings) {
        
        if (null == findings || findings.isEmpty() || null == normFindings || normFindings.isEmpty()) {
            throw new IllegalArgumentException("no findings");
        }
        this.pipeline = pipeline;
        this.source = source;
        this.findings = findings;
        this.normalizedFindings = normFindings;
    }
    
    /**
     * Creates a simple finding map for a single finding.
     * 
     * @param key the key/term causing the event
     * @param deviation the deviation in volume indicating a problem
     * @return the finding map
     */
    private static Map<String, Double> createSingleFinding(String key, double deviation) {
        Map<String, Double> result = new HashMap<String, Double>();
        result.put(key, deviation);
        return result;
    }
    
    @Override
    public String getPipeline() {
        return pipeline;
    }
    
    /**
     * Returns the pipeline source where the volume change occurs.
     * 
     * @return the the source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Returns the findings.
     * 
     * @return the findings
     */
    public Map<String, Double> getFindings() {
        return findings;
    }
    
    /**
     * Returns the normalized findings.
     * 
     * @return the findings
     */
    public Map<String, Double> getNormalizedFindings() {
        return normalizedFindings;
    }
    
    /**
     * Returns the sum of all deviations.
     * 
     * @return the sum of all deviations
     */
    public double getAllDeviations() {
        double result = 0;
        for (Double dev : findings.values()) {
            result += dev;
        }
        return result;
    }

    /**
     * Returns the average of all deviations.
     * 
     * @return the average of all deviations
     */
    public double getAllNormalizedDeviations() {
        double result = 0;
        if (normalizedFindings.size() > 0) {
            for (Double dev : normalizedFindings.values()) {
                result += dev;
            }
            result = result / normalizedFindings.size();
        }
        return result;
    }

}
