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

import eu.qualimaster.adaptation.AdaptationManager;
import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.monitoring.MonitoringManager;
import tests.eu.qualimaster.coordination.AbstractCoordinationTests;

/**
 * Abstract test for setting up/tearing down the adaptation layer.
 * 
 * @author Holger Eichelberger
 */
public class AbstractAdaptationTests extends AbstractCoordinationTests {

    /**
     * Executed before a single test.
     */
    @Before
    public void setUp() {
        super.setUp();
        DataManager.start(true);
        MonitoringManager.start();
        // force a matching authentication provider for the tests
        AdaptationManager.setAuthenticationProvider(TestAuthenticationSupport.PROVIDER);
        AdaptationManager.start();
    }
    
    /**
     * Executed after a single test.
     */
    @After
    public void tearDown() {
        DataManager.stop();
        AdaptationManager.stop();
        MonitoringManager.stop();
        super.tearDown();
    }

}
