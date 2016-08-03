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
package tests.eu.qualimaster.common;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import tests.eu.qualimaster.common.signal.PortManagerTest;

/**
 * The tests for the storm commons component.
 * 
 * @author Holger Eichelberger
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({tests.eu.qualimaster.common.signal.AllTests.class, 
    RecordingTopologyBuilderTest.class, OutputItemsTest.class, HardwareConnectionTest.class, 
    AlgorithmUtilsTest.class, KryoTupleSerializerTest.class, TupleReceiverServerTest.class, 
    PortManagerTest.class })
public class AllTests {

}
