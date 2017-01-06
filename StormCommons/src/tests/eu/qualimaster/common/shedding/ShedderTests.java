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
package tests.eu.qualimaster.common.shedding;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.common.shedding.DefaultLoadShedders;
import eu.qualimaster.common.shedding.DefaultLoadSheddingParameter;
import eu.qualimaster.common.shedding.FairPatternShedder100;
import eu.qualimaster.common.shedding.ILoadShedderConfigurer;
import eu.qualimaster.common.shedding.ILoadShedderDescriptor;
import eu.qualimaster.common.shedding.ILoadSheddingParameter;
import eu.qualimaster.common.shedding.LoadShedder;
import eu.qualimaster.common.shedding.LoadShedderFactory;
import eu.qualimaster.common.shedding.NthItemShedder;
import eu.qualimaster.common.shedding.ProbabilisticShedder;
import eu.qualimaster.common.signal.LoadSheddingSignal;

/**
 * Some shedder tests.
 * 
 * @author Holger Eichelberger
 */
public class ShedderTests {
    
    /**
     * Tests the creation of the no shedder.
     */
    @Test
    public void testNoShedder() {
        Assert.assertNotNull(LoadShedderFactory.createShedder(DefaultLoadShedders.NO_SHEDDING));
    }
    
    /**
     * Creates a load shedder configurer for one parameter/value.
     * 
     * @param param the parameter
     * @param value the value
     * @return the configurer
     */
    private static ILoadShedderConfigurer createConfigurer(ILoadSheddingParameter param, Serializable value) {
        Map<String, Serializable> parameter = new HashMap<String, Serializable>();
        parameter.put(param.name(), value);
        return new LoadSheddingSignal("", "", "", parameter, "");
    }

    /**
     * Tests the behavior of the nth-item shedder.
     */
    @Test
    public void testNthItemShedder() {
        testNthItemShedder(0);
        testNthItemShedder(1);
        testNthItemShedder(2);
        testNthItemShedder(3);
    }

    /**
     * Tests the nth-item shedder.
     * 
     * @param nthItem the n-th item
     */
    private void testNthItemShedder(int nthItem) {
        LoadShedder<?> shedder = LoadShedderFactory.createShedder(DefaultLoadShedders.NTH_ITEM);
        Assert.assertNotNull(shedder);
        shedder.configure(createConfigurer(DefaultLoadSheddingParameter.NTH_TUPLE, nthItem));
        int count = 10;
        int enabled = 0;
        for (int i = 0; i < count; i++) {
            if (shedder.isEnabled(i)) {
                enabled++;
            }
        }
        if (nthItem <= 0) {
            Assert.assertEquals(count, enabled);
        } else if (nthItem == 1) {
            Assert.assertEquals(0, enabled);
        } else {
            int expected = Math.abs((count - enabled) - (count / nthItem));
            Assert.assertTrue("nth " + nthItem + " enabled " + enabled + " count " + count + " expected " 
                + expected, expected <= 1);
        }
    }

    /**
     * Tests the event-based creation of the fair pattern shedder.
     */
    @Test
    public void testNthItemShedderCreation() {
        assertShedderCreation(DefaultLoadShedders.NTH_ITEM.name(), 
            DefaultLoadSheddingParameter.NTH_TUPLE.name(), 3, NthItemShedder.class);
        assertShedderCreation(DefaultLoadShedders.NTH_ITEM.getIdentifier(), 
            DefaultLoadSheddingParameter.NTH_TUPLE.name(), 4, NthItemShedder.class);
        assertShedderCreation(NthItemShedder.class.getName(), 
            DefaultLoadSheddingParameter.NTH_TUPLE.name(), 5, NthItemShedder.class);
    }

    /**
     * Tests the probabilistic shedder.
     */
    @Test
    public void testProbabilisticShedder() {
        testProbabilisticShedder(0.0);
        testProbabilisticShedder(0.1);
        testProbabilisticShedder(0.5);
        testProbabilisticShedder(1.0);
    }

    /**
     * Tests the probabilistic shedder.
     * 
     * @param probability the shedder probability
     */
    private void testProbabilisticShedder(double probability) {
        LoadShedder<?> shedder = LoadShedderFactory.createShedder(DefaultLoadShedders.PROBABILISTIC);
        Assert.assertNotNull(shedder);
        shedder.configure(createConfigurer(DefaultLoadSheddingParameter.PROBABILITY, probability));
        int count = 10;
        int enabled = 0;
        for (int i = 0; i < count; i++) {
            if (shedder.isEnabled(i)) {
                enabled++;
            }
        }
        if (probability <= 0) {
            Assert.assertEquals(count, enabled);
        } else {
            int expected = (int) Math.abs((count - enabled) - (count * probability));
            // more difference due to probabilistic
            Assert.assertTrue("enabled " + enabled + " count " + count + " expected " + expected, expected <= 4);
        }
    }

    /**
     * Tests the event-based creation of the fair pattern shedder.
     */
    @Test
    public void testProbabilisticShedderCreation() {
        assertShedderCreation(DefaultLoadShedders.PROBABILISTIC.name(), 
             DefaultLoadSheddingParameter.PROBABILITY.name(), 0.3, ProbabilisticShedder.class);
        assertShedderCreation(DefaultLoadShedders.PROBABILISTIC.getIdentifier(), 
             DefaultLoadSheddingParameter.PROBABILITY.name(), 0.4, ProbabilisticShedder.class);
        assertShedderCreation(ProbabilisticShedder.class.getName(), 
             DefaultLoadSheddingParameter.PROBABILITY.name(), 0.5, ProbabilisticShedder.class);
    }
    
    /**
     * Tests the fair pattern shedder.
     */
    @Test
    public void testFairPatternShedder100() {
        testFairPatternShedder100(0.0);
        testFairPatternShedder100(0.1);
        testFairPatternShedder100(0.21);
        testFairPatternShedder100(0.3);
        testFairPatternShedder100(0.5);
        testFairPatternShedder100(0.75);
        testFairPatternShedder100(1.0);
    }

    /**
     * Tests the fair pattern shedder with pattern size 100.
     * 
     * @param probability the shedder probability
     */
    private void testFairPatternShedder100(double probability) {
        testFairPatternShedder(probability, DefaultLoadShedders.FAIR_PATTERN, 250);
    }
    
    /**
     * Tests the probabilistic shedder.
     * 
     * @param probability the shedder probability
     * @param descriptor the shedder descriptor (an {@link AbstractFairPatternShedder}).
     * @param count the number of items to test
     */
    private void testFairPatternShedder(double probability, ILoadShedderDescriptor descriptor, int count) {
        LoadShedder<?> shedder = LoadShedderFactory.createShedder(descriptor);
        Assert.assertNotNull(shedder);
        shedder.configure(createConfigurer(DefaultLoadSheddingParameter.RATIO, probability));
        int enabled = 0;
        for (int i = 0; i < count; i++) {
            if (shedder.isEnabled(i)) {
                enabled++;
            }
        }
        if (probability <= 0) {
            Assert.assertEquals(count, enabled);
        } else {
            int expected = (int) Math.abs((count - enabled) - (count * probability));
            Assert.assertTrue("enabled " + enabled + " count " + count + " expected " + expected, expected <= 2);
        }
    }
    
    /**
     * Tests the event-based creation of the fair pattern shedder.
     */
    @Test
    public void testFairPatternShedder100Creation() {
        assertShedderCreation(DefaultLoadShedders.FAIR_PATTERN.name(), 
            DefaultLoadSheddingParameter.RATIO.name(), 0.3, FairPatternShedder100.class);
        assertShedderCreation(DefaultLoadShedders.FAIR_PATTERN.getIdentifier(), 
            DefaultLoadSheddingParameter.RATIO.name(), 0.4, FairPatternShedder100.class);
        assertShedderCreation(FairPatternShedder100.class.getName(), 
            DefaultLoadSheddingParameter.RATIO.name(), 0.5, FairPatternShedder100.class);
    }
    
    /**
     * Asserts a shedder creation. Intentionally use strings.
     * 
     * @param shedder the shedder name
     * @param parameter the shedder parameter
     * @param paramValue the parameter value
     * @param cls the expected class
     */
    private void assertShedderCreation(String shedder, String parameter, Serializable paramValue, 
        Class<? extends LoadShedder<?>> cls) {
        // intentionally use strings, "go" the way over signal as in the infrastructure
        Map<String, Serializable> param = new HashMap<String, Serializable>();
        param.put(parameter, paramValue);
        LoadSheddingSignal signal = new LoadSheddingSignal("", "", shedder, param, null);
        System.out.println(signal);

        LoadShedder<?> shedderInst = LoadShedderFactory.createShedder(signal.getShedder());
        Assert.assertTrue("wrong instance " + shedderInst.getClass().getName(), cls.isInstance(shedderInst));
        shedderInst.configure(signal);
        Assert.assertEquals(param, shedderInst.getStringConfiguration());
    }

}
