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
package eu.qualimaster.monitoring.systemState;

/**
 * Modifies a monitored value in case of overload by a multiplicative factor.
 * 
 * @author Holger Eichelberger
 */
public class MultiplicativeOverloadModifer implements IOverloadModifier {

    private double factor;
    
    /**
     * Creates a multiplicative overload modifier.
     * 
     * @param factor the factor
     */
    public MultiplicativeOverloadModifer(double factor) {
        this.factor = factor;
    }
    
    @Override
    public double modify(double value) {
        return value * factor;
    }
    
    @Override
    public String toString() {
        return "* " + factor;
    }

}
