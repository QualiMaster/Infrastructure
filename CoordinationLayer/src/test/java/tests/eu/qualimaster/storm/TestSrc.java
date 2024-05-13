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

import eu.qualimaster.dataManagement.sources.GenericMultiSourceHandler;
import eu.qualimaster.dataManagement.sources.IDataSourceListener;
import eu.qualimaster.dataManagement.sources.IHistoricalDataProvider;
import eu.qualimaster.dataManagement.sources.ReplayMechanism;
import eu.qualimaster.dataManagement.sources.replay.LongTimestampParser;
import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;
import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.EndOfDataEvent;
import eu.qualimaster.observables.IObservable;

/**
 * Implements the real data source.
 * 
 * @author Holger Eichelberger
 */
public class TestSrc implements ISrc {
    
    public static final String PIP_NAME = "TestPipeline";
    
    private GenericMultiSourceHandler handler = new GenericMultiSourceHandler(1);
    private ReplayMechanism replay = new ReplayMechanism(LongTimestampParser.INSTANCE);
    private boolean eodSent = false;
 
    @Override
    public void connect() {
        replay.connect();
    }

    @Override
    public void disconnect() {
        replay.disconnect();
    }

    @Override
    public void setStrategy(IStorageStrategyDescriptor strategy) {
        replay.setStrategy(strategy);
    }

    @Override
    public IStorageStrategyDescriptor getStrategy() {
        return replay.getStrategy();
    }

    @Override
    public Double getMeasurement(IObservable observable) {
        return replay.getMeasurement(observable);
    }

    @Override
    public Integer getData() {
        Integer result = null;
        if (replay.isEOD()) {
            if (!eodSent) {
                EventManager.send(new EndOfDataEvent(PIP_NAME, AlgorithmProfileHelper.SRC_NAME));
                eodSent = true;
            }
        } else {
            String genericInput = replay.getNext(true);
            if (null != genericInput) {
                char separator = replay.getSeparator();
                result = handler.next("data", Integer.class, genericInput, separator, false, false);
            }
        }
        return result;
    }
    
    @Override
    public String toString() {
        return "Test profiling data source";
    }

    /**
     * Changes the data file.
     * 
     * @param dataFile the data file
     */
    public void setParameterDataFile(String dataFile) {
        replay.setParameterDataFile(dataFile);
    }

    /**
     * Changes the HDFS data file.
     * 
     * @param dataFile the data file
     */
    public void setParameterHdfsDataFile(String dataFile) {
        replay.setParameterHdfsDataFile(dataFile);
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
