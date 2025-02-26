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
package tests.eu.qualimaster.monitoring.profiling;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import tests.eu.qualimaster.monitoring.ProfileReaderTest;

/**
 * The test suite for the Monitoring Profiling support. Do not rename this class.
 * 
 * @author Christopher Voges
 * @author Holger Eichelberger
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    UtilsTest.class,
    InstantiationTest.class, 
    PerformanceTest.class, 
    QualityTest.class,
    QuantizerTest.class,
    ManagerTest.class, 
    SelectionTests.class, 
    ApproximatorTest.class, 
    ProfileReaderTest.class, 
    EventTests.class})
public class ProfilingTests {
}