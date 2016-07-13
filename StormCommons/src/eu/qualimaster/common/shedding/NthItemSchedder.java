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

/**
 * Creates a static shedder for the n-th item passing. Starts with deactivated.
 * 
 * @author Holger Eichelberger
 */
public class NthItemSchedder extends LoadShedder<Object> {

    private static final long serialVersionUID = -3550347502932196122L;
    private int counter;
    private int limit;

    /**
     * Creates a shedder instance.
     */
    public NthItemSchedder() {
        super(DefaultLoadShedders.NTH_ITEM, Object.class, DefaultLoadSheddingParameter.NTH_TUPLE);
    }

    @Override
    protected boolean isEnabledImpl(Object tuple) {
        boolean result;
        if (limit >= 0) {
            counter++;
            if (counter != limit) {
                result = true;
            } else {
                result = false;
                counter = 0;
            }
        } else { // illegal limit, let all pass
            result = true;
        }
        return result;
    }
    
    @Override
    public void configure(ILoadShedderConfigurer configurer) {
        limit = configurer.getIntParameter(DefaultLoadSheddingParameter.NTH_TUPLE, 0);
    }

}
