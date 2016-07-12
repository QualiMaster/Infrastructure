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
package tests.eu.qualimaster.storm;

import java.util.Map;

import eu.qualimaster.easy.extension.internal.AlgorithmProfileHelper;
import eu.qualimaster.infrastructure.PipelineOptions;

/**
 * A test profiling source.
 * 
 * @author Holger Eichelberger
 */
public class TestSource extends Source<TestSrc> {

    private static final long serialVersionUID = 7847255366960713606L;

    /**
     * Creates a test source instance.
     * 
     * @param pipeline the pipeline name
     */
    public TestSource(String pipeline) {
        super(TestSrc.class, pipeline);
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    protected void initializeParams(Map stormConf, TestSrc source) {
        super.initializeParams(stormConf, source);
        TestSrc src = getSource();
        src.setParameterDataFile(PipelineOptions.getExecutorStringArgument(stormConf, 
            AlgorithmProfileHelper.SRC_NAME, AlgorithmProfileHelper.PARAM_DATAFILE, ""));
        src.setParameterHdfsDataFile(PipelineOptions.getExecutorStringArgument(stormConf, 
            AlgorithmProfileHelper.SRC_NAME, AlgorithmProfileHelper.PARAM_HDFS_DATAFILE, ""));
    }

}
