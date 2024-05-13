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
package eu.qualimaster.monitoring;

import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.coordination.RuntimeVariableMapping;
import eu.qualimaster.easy.extension.internal.PipelineHelper;
import eu.qualimaster.easy.extension.internal.VariableHelper;
import eu.qualimaster.monitoring.events.FrozenSystemState;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.model.ModelQueryException;

/**
 * Common pipeline monitoring utility functions.
 * 
 * @author Holger Eichelberger
 */
public class PipelineUtils {

    /**
     * Appends the logical name of <code>var</code> (if present) to <code>path</code>.
     * 
     * @param path the path to be built (modified as a side effect)
     * @param var the variable the name shall be appended for
     * @return the logical name of <code>var</code> or <b>null</b> if it does not exist
     */
    public static String appendName(List<String> path, IDecisionVariable var) {
        return appendName(path, VariableHelper.getName(var));
    }

    /**
     * Appends the logical name of <code>var</code> (if present) to <code>path</code>.
     * 
     * @param path the path to be built (modified as a side effect)
     * @param name the name that shall be appended for
     * @return <code>name</code>
     */
    public static String appendName(List<String> path, String name) {
        if (null != name) {
            path.add(0, name);
        }
        return name;
    }

    /**
     * Constructs a path from the given <code>pipeline</code> and <code>pipelineElement</code>.
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @return the path
     */
    public static List<String> constructPath(String pipeline, String pipelineElement) {
        List<String> path = new ArrayList<String>();
        appendName(path, pipelineElement);
        appendName(path, pipeline);
        return path;
    }

    /**
     * Turns the given names into a path/qualified name of {@link FrozenSystemState}.
     * 
     * @param path the path
     * @return the combined name
     */
    public static String toFrozenStatePath(List<String> path) {
        StringBuilder result = new StringBuilder("");
        if (null != path) {
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) {
                    result.append(FrozenSystemState.SEPARATOR);
                }
                result.append(path.get(i));
            }
        }
        return result.toString();
    }

    /**
     * Constructs a path from the given <code>pipeline</code> and <code>pipelineElement</code>.
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @return the path
     */
    public static String toFrozenStatePathString(String pipeline, String pipelineElement) {
        return toFrozenStatePath(constructPath(pipeline, pipelineElement));
    }
    
    /**
     * Determines a frozen state path.
     * 
     * @param var the variable to determine the path for
     * @param mapping the runtime variable mapping 
     * @return the frozen state path
     */
    public static List<String> determineFrozenStatePath(IDecisionVariable var, RuntimeVariableMapping mapping) {
        // if possible, follow paths up (in particular pipeline)
        IDecisionVariable top = Configuration.getTopLevelDecision(var);
        List<String> path = new ArrayList<String>();
        appendName(path, top);
        IDecisionVariable pipElt = mapping.getReferencedBy(top);
        if (null == pipElt) { // detailed: null != pipElt -> appendName(path, pipElt); 
            pipElt = top;
        }
        try {
            IDecisionVariable pipeline = PipelineHelper.obtainPipeline(var.getConfiguration(), pipElt);
            if (null != pipeline) {
                appendName(path, pipeline);
                // -> element in pipeline
            }
        } catch (ModelQueryException e) {
        }
        return path;
    }

}
