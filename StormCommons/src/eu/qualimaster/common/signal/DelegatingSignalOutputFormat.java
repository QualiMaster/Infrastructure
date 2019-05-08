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

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobConfigurable;
import org.apache.hadoop.mapred.OutputFormat;
import org.apache.hadoop.mapred.RecordWriter;
import org.apache.hadoop.util.Progressable;

import eu.qualimaster.common.signal.AlgorithmSignalHandler.IAlgorithmHolder;

/**
 * An input format acting as a basic Hadoop signal sink.
 * 
 * @param <T> the sink interface type
 * @param <K> the key type
 * @param <V> the value type
 * @author Holger Eichelberger
 */
public class DelegatingSignalOutputFormat<T extends OutputFormat<K, V>, K, V> extends DefaultHadoopSignalReceiver<T> 
    implements OutputFormat<K, V>, JobConfigurable, IAlgorithmHolder<T> {

    private static final long serialVersionUID = 7357406882786827505L;
    private transient T sink;
    private HadoopSignalHandler signalHandler;
    
    /**
     * Creates an instance.
     */
    public DelegatingSignalOutputFormat() {
        initialize(this);
    }

    @Override
    public RecordWriter<K, V> getRecordWriter(FileSystem ignored, JobConf job, String name, Progressable progress)
        throws IOException {
        getAlgorithmHandler().execute();
        RecordWriter<K, V> result = null;
        if (null != sink) {
            result = sink.getRecordWriter(ignored, job, name, progress);
        }
        return result;
    }

    @Override
    public void checkOutputSpecs(FileSystem ignored, JobConf job) throws IOException {
        if (null != sink) {
            sink.checkOutputSpecs(ignored, job);
        }
    }

    @Override
    public void configure(JobConf job) {
        if (sink instanceof JobConfigurable) {
            ((JobConfigurable) sink).configure(job);
        }
        signalHandler = configure(HadoopSignalHandler.PREFIX_SINK, job, this);
    }
    
    /**
     * Adds a configurer.
     * 
     * @param job the job configuration to modify
     * @param cls the configurer class
     */
    public static void setConfigurer(JobConf job, Class<? extends IConfigurer<?>> cls) {
        if (null != cls) {
            job.set(HadoopSignalHandler.getConfigurerKey(HadoopSignalHandler.PREFIX_SINK), cls.getName());
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
        return sink;
    }

    @Override
    public void setCurrentAlgorithm(T sink) {
        this.sink = sink;
    }

}
