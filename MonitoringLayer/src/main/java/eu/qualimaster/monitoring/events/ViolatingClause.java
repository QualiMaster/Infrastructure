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
package eu.qualimaster.monitoring.events;

import java.io.Serializable;

import eu.qualimaster.adaptation.events.IPipelineAdaptationEvent;
import eu.qualimaster.common.QMInternal;
import eu.qualimaster.common.QMSupport;
import eu.qualimaster.observables.IObservable;

/**
 * Represents a violating constraint clause.
 * 
 * @author Holger Eichelberger
 */
@QMSupport
public class ViolatingClause implements Serializable, IPipelineAdaptationEvent {
    
    public static final double CLEARED = Double.MAX_VALUE;
    private static final long serialVersionUID = 2796443137321913100L;
    private String variable;
    private String operation;
    private Double deviation;
    private Double devationPercentage;
    private IObservable observable;
    private String pipeline;
    
    /**
     * Creates a violating clause.
     * 
     * @param observable the observable causing the violation (may be <b>null</b> if unknown)
     * @param variable the variable causing the violation, i.e., the name of the variable
     * @param operation the constraint operation causing the violation (may be <b>null</b> if unknown)
     * @param deviation the actual deviation (may be <b>null</b> if unknown)
     * @param derivationPercentage the actual derivation in percentage (may be <b>null</b> if unknown, may be NaN)
     */
    @QMInternal
    public ViolatingClause(IObservable observable, String variable, String operation, Double deviation, 
        Double derivationPercentage) {
        this.variable = variable;
        this.operation = operation;
        this.deviation = deviation;
        this.devationPercentage = derivationPercentage;
        this.observable = observable;
    }
    
    /**
     * Sets the optional pipeline name.
     * 
     * @param pipeline the pipeline name
     */
    public void setPipeline(String pipeline) {
        this.pipeline = pipeline;
    }
    
    @Override
    public String getPipeline() {
        return pipeline;
    }
    
    /**
     * Returns the causing observable.
     * 
     * @return the causing observable (may be <b>null</b> if unknown)
     */
    public IObservable getObservable() {
        return observable;
    }
    
    /**
     * The variable (name).
     * 
     * @return the variable name
     */
    public String getVariable() {
        return variable;
    }
    
    /**
     * The operation.
     * 
     * @return the operation
     */
    public String getOperation() {
        return operation;
    }
    
    /**
     * Returns the deviation.
     * 
     * @return the deviation (may be <b>null</b> if unknown)
     */
    public Double getDeviation() {
        return deviation;
    }
    
    /**
     * Returns the deviation percentage.
     * 
     * @return the deviation percentage (may be <b>null</b> if unknown, may be NaN)
     */
    public Double getDeviationPercentage() {
        return devationPercentage;
    }
    
    /**
     * Whether a previous violation shall be considered to be cleared / not relevant anymore.
     * 
     * @return <code></code>
     */
    public boolean isCleared() {
        return (deviation != null && deviation == CLEARED) 
            || (devationPercentage != null && devationPercentage == CLEARED);
    }

    @Override
    public String toString() {
        return "violating clause [obs " + observable + " var "  + variable + " op " + operation + " dev " 
            + deviation + " " + devationPercentage + "% " + isCleared() + "]";
    }

}