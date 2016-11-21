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
package tests.eu.qualimaster;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.infrastructure.PipelineOptions;

/**
 * Performs additional tests for the pipeline options.
 * 
 * @author Holger Eichelberger
 */
public class PipelineOptionsTest {
    
    /**
     * Tests the pipeline options.
     */
    @Test
    public void pipelineOptionsTest() {
        PipelineOptions options = new PipelineOptions();
        Assert.assertEquals(7, options.getNumberOfWorkers(7));
        Assert.assertEquals(4, options.getWaitTime(4));
        Assert.assertEquals(3, options.getExecutorParallelism("exec", 3));
        Assert.assertEquals(2, options.getTaskParallelism("exec", 2));
        Assert.assertFalse(options.isInProfilingMode());
        Assert.assertNull(options.getMainPipeline());
        Assert.assertFalse(options.isSubPipeline());
        assertThroughArgs(options);

        options.setOption("myKey", "1");
        Assert.assertEquals("1", options.getOption("myKey"));

        options.markAsSubPipeline("");
        Assert.assertEquals("", options.getMainPipeline());
        Assert.assertFalse(options.isSubPipeline());
        options.markAsSubPipeline("mainPip");
        Assert.assertEquals("mainPip", options.getMainPipeline());
        Assert.assertTrue(options.isSubPipeline());

        options.setNumberOfWorkers(5);
        Assert.assertEquals(5, options.getNumberOfWorkers(2));
        assertThroughArgs(options);
        
        options.setWaitTime(10);
        Assert.assertEquals(5, options.getNumberOfWorkers(2));
        Assert.assertEquals(10, options.getWaitTime(2));
        assertThroughArgs(options);
        
        options.setExecutorParallelism("bla", 3);
        Assert.assertEquals(3, options.getExecutorParallelism("bla", 10));
        assertThroughArgs(options);

        options.setExecutorParallelism("bli", 2);
        Assert.assertEquals(3, options.getExecutorParallelism("bla", 10));
        Assert.assertEquals(2, options.getExecutorParallelism("bli", 10));
        assertThroughArgs(options);
        
        options.setTaskParallelism("bla", 3);
        options.setTaskParallelism("bli", 2);
        options.enableProfilingMode();
        Assert.assertEquals(3, options.getTaskParallelism("bla", 10));
        Assert.assertEquals(2, options.getTaskParallelism("bli", 10));
        Assert.assertTrue(options.isInProfilingMode());
        assertThroughArgs(options);
        
        // int works also, but comparison goes via string and not back
        options.setExecutorArgument("exec2", "delay", "2000");
        // these two should not cause a difference
        options.setExecutorArgument(null, "delay", "2000");
        options.setExecutorArgument("exec2", null, "2000");
        Assert.assertEquals("2000", options.getExecutorArgument("exec2", "delay"));
        assertThroughArgs(options);
        
        Assert.assertNull(null, options.getExecutorArgument(null, "delay"));
        Assert.assertNull(null, options.getExecutorArgument("exec2", null));
        
        options.toString(); // actually no test needed
        Map<Object, Object> c = new HashMap<Object, Object>();
        options.toConf(c);
        
        PipelineOptions n = new PipelineOptions(options);
        assertThroughArgs(n);
        Assert.assertEquals(options, n);
        n = new PipelineOptions();
        n.merge(options);
        assertThroughArgs(n);
        Assert.assertEquals(options, n);
    }
    
    /**
     * Tests the executor argument access helpers.
     */
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void execArgTests() {
        PipelineOptions options = new PipelineOptions();
        options.setExecutorArgument("exec2", "delay", "2000");

        Map conf = options.toMap();
        Assert.assertEquals(2000, PipelineOptions.getExecutorIntArgument(conf, "exec2", "delay", 0));
        Assert.assertEquals(0, PipelineOptions.getExecutorIntArgument(conf, "exec*", "delay", 0));
        Assert.assertEquals("2000", PipelineOptions.getExecutorStringArgument(conf, "exec2", "delay", ""));
        Assert.assertEquals("", PipelineOptions.getExecutorStringArgument(conf, "exec*", "delay", ""));
        Assert.assertEquals(1.0, PipelineOptions.getExecutorDoubleArgument(conf, "exec*", "delay", 1.0), 0.05);
        Assert.assertEquals(false, PipelineOptions.getExecutorBooleanArgument(conf, "exec*", "delay", false));
        String key = PipelineOptions.getExecutorArgumentKey("exec2", "delay");
        
        conf.put(key, 2000);
        Assert.assertEquals(2000, PipelineOptions.getExecutorIntArgument(conf, "exec2", "delay", 0));
        Assert.assertEquals(2000, PipelineOptions.getExecutorLongArgument(conf, "exec2", "delay", 0));
        Assert.assertEquals(2000.0, PipelineOptions.getExecutorDoubleArgument(conf, "exec2", "delay", 0.0), 0.05);
        conf.put(key, 2000.0);
        Assert.assertEquals(0, PipelineOptions.getExecutorIntArgument(conf, "exec2", "delay", 0));
        Assert.assertEquals(0, PipelineOptions.getExecutorLongArgument(conf, "exec2", "delay", 0));
        Assert.assertEquals(2000.0, PipelineOptions.getExecutorDoubleArgument(conf, "exec2", "delay", 0.0), 0.05);
        conf.put(key, "aaa");
        Assert.assertEquals(-1, PipelineOptions.getExecutorIntArgument(conf, "exec2", "delay", -1));
        Assert.assertEquals(0.0, PipelineOptions.getExecutorDoubleArgument(conf, "exec2", "delay", 0.0), 0.05);
        Assert.assertEquals(false, PipelineOptions.getExecutorBooleanArgument(conf, "exec2", "delay", false));
        conf.put(key, "true");
        Assert.assertEquals(true, PipelineOptions.getExecutorBooleanArgument(conf, "exec2", "delay", false));
        conf.put(key, Boolean.TRUE);
        Assert.assertEquals(true, PipelineOptions.getExecutorBooleanArgument(conf, "exec2", "delay", false));
        conf.put(key, 100L);
        Assert.assertEquals(100L, PipelineOptions.getExecutorLongArgument(conf, "exec2", "delay", 0));
    }

    /**
     * Tests the executor parallelism access helpers.
     */
    @Test
    @SuppressWarnings({ "rawtypes" })
    public void parallelismArgTests() {
        PipelineOptions options = new PipelineOptions();
        options.setExecutorParallelism("exec2", 5);
        options.setTaskParallelism("exec1", 4);
        options.setNumberOfWorkers(3);

        Map conf = options.toMap();
        Assert.assertEquals(0, PipelineOptions.getExecutorParallelism(conf, "exec1", 0));
        Assert.assertEquals(5, PipelineOptions.getExecutorParallelism(conf, "exec2", 0));
        Assert.assertEquals(4, PipelineOptions.getTaskParallelism(conf, "exec1", 0));
        Assert.assertEquals(0, PipelineOptions.getTaskParallelism(conf, "exec2", 0));
        Assert.assertEquals(3, PipelineOptions.getNumberOfWorkers(conf, 0));
    }
    
    /**
     * Asserts the equality of <code>options</code> with its arguments-parsed counterpart.
     * 
     * @param options the options to be teste4d
     */
    private void assertThroughArgs(PipelineOptions options) {
        PipelineOptions opt = new PipelineOptions(options.toArgs(null));
        Assert.assertEquals(opt, options);
        Assert.assertEquals(opt.hashCode(), options.hashCode());
        
        // shall not change result
        PipelineOptions opt2 = new PipelineOptions(options.toArgs("me"));
        Assert.assertEquals(opt2, options);
        Assert.assertEquals(opt2.hashCode(), options.hashCode());
        
        Assert.assertEquals(opt2, opt);
        Assert.assertEquals(opt2.hashCode(), opt.hashCode());
    }

    /**
     * Tests the adaptation filter.
     */
    @Test
    public void testAdaptationFilter() {
        assertAdaptationFilter(null);
        assertAdaptationFilter(AdaptationEvent.class);
    }

    /**
     * Asserts the adaptation filter <code>cls</code> on a new {@link PipelineOptions} instance.
     * 
     * @param cls the adaptation filter (may be <b>null</b>)
     */
    private void assertAdaptationFilter(Class<? extends AdaptationEvent> cls) {
        PipelineOptions opts = new PipelineOptions(cls);
        assertAdaptationFilterWithArgs(opts, cls);
    }

    /**
     * Asserts the adaptation filter with writing the options to pipeline command line
     * arguments and reading it back.
     * 
     * @param options the pipeline options
     * @param expected the expected adaptation filter to be asserted (may be <b>null</b>)
     */
    private void assertAdaptationFilterWithArgs(PipelineOptions options, Class<? extends AdaptationEvent> expected) {
        assertAdaptationFilter(options, expected);
        String[] args = options.toArgs(null);
        PipelineOptions opts = new PipelineOptions(args);
        assertAdaptationFilter(opts, expected);
    }

    /**
     * Asserts the adaptation filter on the given pipeline options.
     * 
     * @param options the pipeline options
     * @param expected the expected adaptation filter to be asserted (may be <b>null</b>)
     */
    private void assertAdaptationFilter(PipelineOptions options, Class<? extends AdaptationEvent> expected) {
        if (null == expected) {
            Assert.assertNull(options.getAdaptationFilter());
        } else {
            Assert.assertEquals(expected, options.getAdaptationFilter());
        }
    }
    
}
