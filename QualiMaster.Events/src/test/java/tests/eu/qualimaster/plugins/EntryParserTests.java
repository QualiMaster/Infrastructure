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
package tests.eu.qualimaster.plugins;

import org.junit.Test;

import eu.qualimaster.plugins.EntryParser;

import org.junit.Assert;

/**
 * Tests the entry parser.
 * 
 * @author Holger Eichelberger
 */
public class EntryParserTests {

    /**
     * Tests the entry parser.
     */
    @Test
    public void testEntryParser() {
        assertPluginResult("");
        assertPluginResult("aaa", "aaa");
        assertPluginResult("de.qm.Test", "de.qm.Test");
        assertPluginResult("de.qm.Test[MON]", "de.qm.Test", "MON");
        assertPluginResult("de.qm.Test[MON,MON]", "de.qm.Test", "MON");
        assertPluginResult("de.qm.Test[ MON, MON ]", "de.qm.Test", "MON");
        assertPluginResult("de.qm.Test[ MON, ADAPT ]", "de.qm.Test", "MON", "ADAPT");
        assertPluginResult("de.qm.Test[ MON, MON ], de.qm.Test2", 
            new PluginResult("de.qm.Test", "MON"), 
            new PluginResult("de.qm.Test2"));
        assertPluginResult("de.qm.Test[MON,MON], de.qm.Test2[ADAPT]", 
            new PluginResult("de.qm.Test", "MON"), 
            new PluginResult("de.qm.Test2", "ADAPT"));
        assertPluginResult("de.qm.Test[ADAPT,MON], de.qm.Test2[MON, ADAPT]", 
            new PluginResult("de.qm.Test", "ADAPT", "MON"), 
            new PluginResult("de.qm.Test2", "MON", "ADAPT"));
    }
    
    /**
     * Represents a plugin result.
     * 
     * @author Holger Eichelberger
     */
    private static class PluginResult {
        
        private String cls;
        private String startLayer;
        private String endLayer;
        
        /**
         * Creates a plugin result.
         * 
         * @param cls the class name
         * @param layers the start/end layers, start=end if only one is given
         */
        private PluginResult(String cls, String... layers) {
            this.cls = cls;
            if (1 == layers.length) {
                this.startLayer = layers[0];
                this.endLayer = layers[0];
            } else if (2 == layers.length) {
                this.startLayer = layers[0];
                this.endLayer = layers[1];
            }
        }
    }
    
    /**
     * Implements a plugin parser handler for testing.
     * 
     * @author Holger Eichelberger
     */
    private static class Handler implements EntryParser.IPluginEntryHandler {
        
        private PluginResult[] expected;
        private int successfulIndex = -1;
        
        /**
         * Creates a handler with expected results.
         * 
         * @param expected the expected results
         */
        private Handler(PluginResult... expected) {
            this.expected = expected;
        }

        @Override
        public void handle(String cls, String[] layers) {
            int testIndex = successfulIndex + 1;
            if (testIndex < expected.length) {
                PluginResult exp = expected[testIndex];
                Assert.assertEquals(exp.cls, cls);
                if (exp.startLayer != null && exp.endLayer != null) {
                    Assert.assertEquals("Start layer for expected entry " + testIndex + " shall be " + exp.startLayer 
                        + " rather than " + layers[0], exp.startLayer, layers[0]);
                    Assert.assertEquals("End layer for expected entry " + testIndex + " shall be " + exp.endLayer 
                        + " rather than " + layers[1], exp.endLayer, layers[1]);
                } else {
                    Assert.assertEquals("Layers for expected entry " + testIndex + " shall not be given" , 
                        0, layers.length);
                }
                successfulIndex = testIndex;
            } else {
                Assert.fail("More results than expected.");
            }
        }
        
        /**
         * Performs the final assert.
         */
        public void assertFinally() {
            if (successfulIndex < 0) {
                Assert.assertEquals(expected.length, 0);
            } else {
                Assert.assertEquals(expected.length, successfulIndex + 1);
            }
        }

    }
    
    /**
     * Tests whether a single entry was found.
     * 
     * @param entry manifest entry to be parsed
     * @param cls expected class name
     * @param layers expected start/end layer (optional)
     */
    private static void assertPluginResult(String entry, final String cls, final String... layers) {
        assertPluginResult(entry, new PluginResult(cls, layers));
    }

    /**
     * Tests whether none, one or multiple entries were found.
     * 
     * @param entry manifest entry to be parsed
     * @param expected expected entries in sequence (optional)
     */
    private static void assertPluginResult(String entry, PluginResult... expected) {
        Handler handler = new Handler(expected);
        EntryParser.parseManifestPluginEntry(entry, handler);
        handler.assertFinally();
    }
    
}
