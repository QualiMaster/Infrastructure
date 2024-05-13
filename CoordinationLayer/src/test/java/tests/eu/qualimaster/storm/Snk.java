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

import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;
import eu.qualimaster.dataManagement.strategies.NoStorageStrategyDescriptor;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the data sink.
 * 
 * @author Holger Eichelberger
 */
public class Snk implements ISnk {

    private boolean connected;
    
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
    public void emit(Integer value) {
        if (connected) {
            System.out.println("SINK " + value);
        }
    }
    
    @Override
    public String toString() {
        return "Test data sink";
    }

}
