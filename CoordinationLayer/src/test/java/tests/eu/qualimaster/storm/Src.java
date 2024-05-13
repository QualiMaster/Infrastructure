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

import java.util.Map;

import backtype.storm.utils.Utils;
import eu.qualimaster.dataManagement.sources.IDataSourceListener;
import eu.qualimaster.dataManagement.sources.IHistoricalDataProvider;
import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;
import eu.qualimaster.dataManagement.strategies.NoStorageStrategyDescriptor;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the real data source.
 * 
 * @author Holger Eichelberger
 */
public class Src implements ISrc {
    
    private boolean connected = false;
    private int value;
    
    @Override
    public void connect() {
        connected = true;
    }

    @Override
    public void disconnect() {
        connected = false;
    }

    @Override
    public void setStrategy(IStorageStrategyDescriptor strategy) {
        // ignore
    }

    @Override
    public IStorageStrategyDescriptor getStrategy() {
        return NoStorageStrategyDescriptor.INSTANCE;
    }

    @Override
    public Double getMeasurement(IObservable observable) {
        return null;
    }

    @Override
    public Integer getData() {
        Integer result;
        if (connected) {
            result = value++;
            Utils.sleep(100);
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "Test data source";
    }

    @Override
    public IHistoricalDataProvider getHistoricalDataProvider() {
        return null;
    }

    @Override
    public Map<String, String> getIdsNamesMap() {
        return null;
    }

    @Override
    public void setDataSourceListener(IDataSourceListener listener) {
        // no mapping, no listener needed
    }

}
