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
package eu.qualimaster.monitoring.observations;


import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.easy.extension.internal.PipelineHelper;
import eu.qualimaster.easy.extension.internal.VariableHelper;
import eu.qualimaster.monitoring.ObservableMapper;
import eu.qualimaster.observables.IObservable;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;

/**
 * Represents a (dummy) constant observation. Maps the parent observation provider to the pipeline, the 
 * observation provider to a pipeline node and the observable via {@link ObservableMapper} to a variable
 * name. Uses the given default value if no configuration variable is accessible.
 * 
 * @author Holger Eichelberger
 */
public class ConfigurationConstantObservation extends ConstantObservation {

    private static final long serialVersionUID = 4574174253907367679L;

    /**
     * Creates a new no-observation.
     * 
     * @param provider the observation provider
     * @param observable the observable this observation is providing
     * @param deflt the default constant value of the observation
     */
    public ConfigurationConstantObservation(IObservationProvider provider, IObservable observable, double deflt) {
        super(getFromConfiguration(provider, observable, deflt));
    }
    
    /**
     * Returns the value of the corresponding variable from the configuration.
     * 
     * @param provider the observation provider
     * @param observable the observable this observation is providing
     * @param deflt the default constant value of the observation
     * @return the value, if not accessible <code>deflt</code>
     */
    private static double getFromConfiguration(IObservationProvider provider, IObservable observable, 
        double deflt) {
        double result = deflt;
        IObservationProvider parent = provider.getParent();
        if (null != parent) {
            String pipName = parent.getName(); // just assume and let's see
            String nodeName = provider.getName(); // node
            String varName = ObservableMapper.getMappedName(observable);
            Models models =  RepositoryConnector.getModels(Phase.MONITORING);
            if (null != models) {
                Configuration cfg = models.getConfiguration();
                IDecisionVariable pipeline = PipelineHelper.obtainPipelineByName(cfg, pipName);
                IDecisionVariable node = PipelineHelper.obtainPipelineElementByName(pipeline, null, nodeName);
                Double val = VariableHelper.getDouble(node, varName);
                if (null != val) {
                    result = val;
                }
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "Conf[" + getValue() + "]";
    }

}
