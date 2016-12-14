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
import java.util.Arrays;
import java.util.Map;

/**
 * Creates a fair shedding pattern by distributing the items to shed over a pattern array of given size. Calculation
 * effort is only needed during configuring the shedder while shedding itself is just a lookup into the pattern.
 * 
 * @author Claudia Niederee
 * @author Holger Eichelberger
 */
public abstract class AbstractFairPatternShedder extends LoadShedder<Object> {

    private static final long serialVersionUID = -3550347502932196122L;
    private int counter;
    private boolean[] pattern;
    private double shedRatio;

    /**
     * Creates a shedder instance.
     * 
     * @param patternSize the size of the shedder pattern
     */
    public AbstractFairPatternShedder(int patternSize) {
        super(DefaultLoadShedders.FAIR_PATTERN, Object.class, DefaultLoadSheddingParameter.RATIO);
        pattern = new boolean[patternSize];
    }

    @Override
    protected boolean isEnabledImpl(Object tuple) {
        boolean result;
        if (shedRatio > 0) {
            if (counter >= pattern.length) {
                counter = 0;
            }
            result = pattern[counter];
            counter++;
        } else { // illegal limit, let all pass
            result = true;
        }
        return result;
    }
    
    @Override
    public void configure(ILoadShedderConfigurer configurer) {
        shedRatio = configurer.getDoubleParameter(DefaultLoadSheddingParameter.RATIO, 0);
        if (shedRatio <= 0) { // 
            Arrays.fill(pattern, true);
        } else if (shedRatio >= 1.0) { // 
            Arrays.fill(pattern, false);
        } else {
            final int n = pattern.length;
            final int k = (int) (n * shedRatio);
            double ratio = (n - k) / ((double) k);
            
            // calculate only pattern for half of the cases, obtain other by inversion
            boolean value;
            if (ratio >= 1) {
                value = false;
            } else {
                ratio = 1 / ratio;
                value = true;
            }
            Arrays.fill(pattern, !value);
            
            // step up by ratio, set value for each position, consider rounding error and increase step if value is set
            double step = 0;
            double error = 0;
            int pos;
            do {
                step = step + ratio;
                pos = (int) Math.floor(step);
                error = step - pos;
                if (error > 1) {
                    pos++;
                    error--;
                }
                if (pos < n) {
                    pattern[pos] = value;
                    step++;
                }
            } while (step <= n);
        }
        
        // start with new pattern
        counter = 0;
    }

    @Override
    public Map<ILoadSheddingParameter, Serializable> getConfiguration() {
        return getConfiguration(DefaultLoadSheddingParameter.RATIO, shedRatio);
    }

}
