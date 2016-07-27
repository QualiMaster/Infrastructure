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
package tests.eu.qualimaster.adaptation;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import tests.eu.qualimaster.coordination.AbstractCoordinationTests;
import tests.eu.qualimaster.coordination.Utils;

/**
 * Global tests.
 * 
 * @author Holger Eichelberger
 */
public class ManagerTests {

    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        Utils.setModelProvider(Utils.INFRASTRUCTURE_TEST_MODEL_PROVIDER);
        Utils.configure();
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        Utils.dispose();
    }
    
    /**
     * Tests obtaining a model artifact.
     */
    @Test
    public void testRegistryConnectorModels() {
        if (!AbstractAdaptationTests.isJenkins()) {
            AbstractCoordinationTests.testLoadModels();
        }
    }
    
}
