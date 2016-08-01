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
package tests.eu.qualimaster.storm;

import eu.qualimaster.dataManagement.sources.FixedRateSource;
import eu.qualimaster.dataManagement.sources.IHistoricalDataProvider;

/**
 * Implements an alternative data source operating at a fixed rate.
 * 
 * @author Holger Eichelberger
 */
public class Src2 extends FixedRateSource<Integer> implements ISrc {

    private int count = 0;

    /**
     * Creates the source.
     */
    public Src2() {
        super(0, 5);
    }
    
    @Override
    public Integer getData() {
        return getDataImpl();
    }

    @Override
    protected Integer createData() {
        return count++;
    }
    
    @Override
    public String toString() {
        return "Test data source (fixed rate)";
    }
    
    @Override
    public IHistoricalDataProvider getHistoricalDataProvider() {
        return null;
    }
    
}
