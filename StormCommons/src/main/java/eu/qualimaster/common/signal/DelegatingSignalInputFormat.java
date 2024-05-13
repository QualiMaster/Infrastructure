/*
 * Copyright 2009-2019 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.common.signal;

import java.io.IOException;

import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;

import eu.qualimaster.common.signal.AlgorithmSignalHandler.IAlgorithmHolder;

/**
 * An input format acting as a basic Hadoop signal source.
 * 
 * @param <T> the source interface type
 * @param <K> the key type
 * @param <V> the value type
 * @author Holger Eichelberger
 */
public class DelegatingSignalInputFormat<T extends InputFormat<K, V>, K, V> extends DefaultHadoopSignalReceiver<T> 
    implements InputFormat<K, V>, JobConfigurable, IAlgorithmHolder<T> {

    private static final long serialVersionUID = -5380148698751183486L;
    private transient T source;
    private HadoopSignalHandler signalHandler;
    
    /**
     * Creates an instance.
     */
    public DelegatingSignalInputFormat() {
        initialize(this);
    }
    
    @Override
    public InputSplit[] getSplits(JobConf job, int numSplits) throws IOException {
        getAlgorithmHandler().execute();
        InputSplit[] result = null;
        if (source != null) {
            result = source.getSplits(job, numSplits);
        }
        return result;
    }

    @Override
    public RecordReader<K, V> getRecordReader(InputSplit split, JobConf job, Reporter reporter) throws IOException {
        RecordReader<K, V> result = null;
        if (source != null) {
            result = source.getRecordReader(split, job, reporter);
        }
        return result;
    }

    @Override
    public void configure(JobConf job) {
        if (source instanceof JobConfigurable) {
            ((JobConfigurable) source).configure(job);
        }
        signalHandler = configure(HadoopSignalHandler.PREFIX_SOURCE, job, this);
    }
    
    /**
     * Adds a configurer.
     * 
     * @param job the job configuration to modify
     * @param cls the configurer class
     */
    public static void setConfigurer(JobConf job, Class<? extends IConfigurer<?>> cls) {
        if (null != cls) {
            job.set(HadoopSignalHandler.getConfigurerKey(HadoopSignalHandler.PREFIX_SOURCE), cls.getName());
        }
    }
    
    /**
     * Returns the signal handler.
     * 
     * @return the signal handler
     */
    protected HadoopSignalHandler getSignalHandler() {
        return signalHandler;
    }

    @Override
    public T getCurrentAlgorithm() {
        return source;
    }

    @Override
    public void setCurrentAlgorithm(T source) {
        this.source = source;
    }

}
