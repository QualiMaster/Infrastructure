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
package eu.qualimaster.coordination.commands;

/**
 * Create a profiling pipeline for the given family/algorithm.
 * 
 * @author Holger Eichelberger
 */
public class ProfileAlgorithmCommand extends CoordinationCommand {

    private static final long serialVersionUID = -3604508742974169281L;
    private String family;
    private String algorithm;
    
    /**
     * Creates a profile algorithm command.
     * 
     * @param family the family to profile
     * @param algorithm the algorithm within <code>family</code> to profiel
     */
    public ProfileAlgorithmCommand(String family, String algorithm) {
        this.family = family;
        this.algorithm = algorithm;
    }
    
    @Override
    public CoordinationExecutionResult accept(ICoordinationCommandVisitor visitor) {
        return visitor.visitProfileAlgorithmCommand(this);
    }
    
    /**
     * Returns the family to profile.
     * 
     * @return the family
     */
    public String getFamily() {
        return family;
    }
    
    /**
     * Returns the algorithm to profile.
     * 
     * @return the algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

}
