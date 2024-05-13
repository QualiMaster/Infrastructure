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
package eu.qualimaster.common.shedding;

import java.io.Serializable;
import java.util.Map;

/**
 * Creates a static shedder for random shedding if the random number is less than a given percentage / proability. 
 * Starts deactivated.
 * 
 * @author Holger Eichelberger
 */
public class ProbabilisticShedder extends LoadShedder<Object> {

    private static final long serialVersionUID = -8297254413874028411L;
    private double percentage;

    /**
     * Creates a shedder instance.
     */
    public ProbabilisticShedder() {
        super(DefaultLoadShedders.PROBABILISTIC, Object.class, DefaultLoadSheddingParameter.PROBABILITY);
    }

    @Override
    protected boolean isEnabledImpl(Object tuple) {
        boolean result;
        if (percentage > 0) {
            result = Math.random() >= percentage;
        } else { // illegal limit, let all pass
            result = true;
        }
        return result;
    }
    
    @Override
    public void configure(ILoadShedderConfigurer configurer) {
        percentage = Math.min(1, Math.max(0, 
             configurer.getDoubleParameter(DefaultLoadSheddingParameter.PROBABILITY, 0)));
    }

    @Override
    public Map<ILoadSheddingParameter, Serializable> getConfiguration() {
        return getConfiguration(DefaultLoadSheddingParameter.PROBABILITY, percentage);
    }

}
