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
package eu.qualimaster.monitoring.events;

import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.events.AbstractReturnableEvent;

/**
 * An event for requesting a source volume prediction.
 * 
 * @author Holger Eichelberger
 * @author Andrea Ceroni
 */
@QMInternal
public class SourceVolumePredictionRequest extends AbstractReturnableEvent {

    private static final long serialVersionUID = 6941328179172553914L;
    private String pipeline;
    private String source;
    private List<String> keywords;
    
    /**
     * Creates a source volume prediction request for a single keyword.
     * 
     * @param pipeline the pipeline to predict for
     * @param source the source to predict for
     * @param keyword the keyword to predict
     */
    public SourceVolumePredictionRequest(String pipeline, String source, String keyword) {
        this(pipeline, source, createSingleKeywordList(keyword));
    }
    
    /**
     * Creates a source volume prediction request for a set of keywords.
     * 
     * @param pipeline the pipeline to predict for
     * @param source the source to predict for
     * @param keywords the keywords to predict
     */
    public SourceVolumePredictionRequest(String pipeline, String source, List<String> keywords) {
        this.pipeline = pipeline;
        this.source = source;
        this.keywords = keywords;
    }
    
    /**
     * Creates a simple keyword list for the single keyword <code>keyword</code>.
     * 
     * @param keyword the keyword
     * @return a list containing <code>keyword</code>
     */
    private static List<String> createSingleKeywordList(String keyword) {
        List<String> result = new ArrayList<String>();
        result.add(keyword);
        return result;
    }
    
    /**
     * Returns the pipeline name.
     * 
     * @return the pipeline name
     */
    public String getPipeline() {
        return pipeline;
    }
    
    /**
     * Returns the source name.
     * 
     * @return the source name
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Returns the number of keywords.
     * 
     * @return the number of keywords
     */
    public int getKeywordCount() {
        return null == keywords ? 0 : keywords.size();
    }
    
    /**
     * Returns the specified keyword.
     * 
     * @param index the index of the keyword
     * @return the keyword
     * @throws IndexOutOfBoundsException if <code>index &lt; 0 || index &gt;={@link #getKeywordCount()}</code>
     */
    public String getKeyword(int index) {
        if (null == keywords) {
            throw new IndexOutOfBoundsException();
        }
        return keywords.get(index);
    }
    
}
