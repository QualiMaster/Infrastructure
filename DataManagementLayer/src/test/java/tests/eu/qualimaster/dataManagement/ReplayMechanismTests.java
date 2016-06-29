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
package tests.eu.qualimaster.dataManagement;

import java.io.File;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.dataManagement.sources.ReplayMechanism;
import eu.qualimaster.dataManagement.sources.replay.DateTimeTimestampParser;
import eu.qualimaster.dataManagement.sources.replay.FileSource;
import eu.qualimaster.dataManagement.sources.replay.IDataManipulator;
import eu.qualimaster.dataManagement.sources.replay.IReplaySource;
import eu.qualimaster.dataManagement.sources.replay.LongTimestampParser;

/**
 * Tests the replay mechanism. We focus on the OS filesystem rather than HDFS in this class. No HDFS-based 
 * tests for now.
 * 
 * @author Holger Eichelberger
 */
public class ReplayMechanismTests {
    
    private static final File TESTDATA = new File(System.getProperty("qm.base.dir", "."), "testdata");

    /**
     * Summarizes test-relevant data.
     * 
     * @author Holger Eichelberger
     */
    private static class TestData {
        
        private String fileName;
        private String expectedPayload;
        private char separator;
        private long expectedTime;

        /**
         * Creates a test data instance.
         * 
         * @param fileName the relative name of the test file
         * @param expectedPayload the expected payload (do not test if <b>null</b>)
         * @param separator the (expected) separator
         * @param expectedTime the expected total time of replay (ms)
         */
        private TestData(String fileName, String expectedPayload, char separator, long expectedTime) {
            this.fileName = fileName;
            this.expectedPayload = expectedPayload;
            this.separator = separator;
            this.expectedTime = expectedTime;
        }
        
        /**
         * Returns the test file.
         * 
         * @return the test file
         */
        private File getFile() {
            return new File(TESTDATA, fileName);
        }

        /**
         * Returns the expected payload.
         * 
         * @return the expected payload (do not test if <b>null</b>)
         */
        private String getExpectedPayload() {
            return expectedPayload;
        }

        /**
         * Returns the expected separator.
         * 
         * @return the expected separator
         */
        private char getSeparator() {
            return separator;
        }

        /**
         * Returns the expected replay time.
         * 
         * @return the expected replay time (ms)
         */
        private long getExpectedTime() {
            return expectedTime;
        }
        
    }
    
    private static final TestData REPLAY1 = new TestData("replay1.data", "123,144,APL,5", ',', 5000);
    private static final TestData REPLAY2 = new TestData("replay2.data", "123;144;APL;5", ';', 5000);
    private static final TestData REPLAY3 = new TestData("replay3.data", "123,144,APL,5", ',', 5000);
    private static final TestData REPLAY4 = new TestData("replay4.data", null, ',', 5000);
    /**
     * Tests simple replay data with default separator and file source just for compliance with the payload 
     * (all via constructor).
     */
    @Test
    public void testLongTimestampsCommaSeparator() {
        testLongTimestamps(REPLAY1);
    }
    
    /**
     * Tests simple replay data with changed separator and file source just for compliance with the payload 
     * (all via constructor).
     */
    @Test
    public void testLongTimestampsSemicolonSeparator() {
        testLongTimestamps(REPLAY2);
    }

    /**
     * Tests simple replay data file source just for compliance with the payload (all via constructor).
     * 
     * @param data the test data
     */
    private void testLongTimestamps(TestData data) {
        IReplaySource source = new FileSource(data.getFile());
        ReplayMechanism replay = new ReplayMechanism(source, LongTimestampParser.INSTANCE);
        assertReplay(replay, data);
    }
    
    /**
     * Tests simple replay data with default separator and file source just for compliance with the payload 
     * (via delayed source).
     */
    @Test
    public void testLongTimestampsCommaSeparator2() {
        testLongTimestamps2(REPLAY1);
    }

    /**
     * Tests simple replay data with changed separator and file source just for compliance with the payload 
     * (via delayed source).
     */
    @Test
    public void testLongTimestampsSemicolonSeparator2() {
        testLongTimestamps2(REPLAY2);
    }

    /**
     * Tests simple replay data file source just for compliance with the payload (via delayed source).
     * 
     * @param data the test data
     */
    private void testLongTimestamps2(TestData data) {
        IReplaySource source = new FileSource(data.getFile());
        ReplayMechanism replay = new ReplayMechanism(LongTimestampParser.INSTANCE);
        replay.setSource(source);
        assertReplay(replay, data);
    }
    
    /**
     * Tests simple replay data with default separator and file source just for compliance with the 
     * payload (via parameter).
     */
    @Test
    public void testLongTimestampsCommaSeparator3() {
        testLongTimestamps3(REPLAY1);
    }
    
    /**
     * Tests simple replay data with changed separator and file source just for compliance with the 
     * payload (via parameter).
     */
    @Test
    public void testLongTimestampsSemicolonSeparator3() {
        testLongTimestamps3(REPLAY2);
    }

    /**
     * Tests simple replay data with file source just for compliance with the payload (via parameter).
     * 
     * @param data the test data
     */
    private void testLongTimestamps3(TestData data) {
        ReplayMechanism replay = new ReplayMechanism(LongTimestampParser.INSTANCE);
        replay.setParameterDataFile(data.getFile().getAbsolutePath());
        assertReplay(replay, data);
    }

    /**
     * Asserts the data.
     * 
     * @param data the test data
     */
    private void assertReplay(ReplayMechanism replay, TestData data) {
        replay.connect();
        int count = 0;
        Assert.assertEquals(data.getSeparator(), replay.getSeparator());
        long start = System.currentTimeMillis();
        String expectedPayload = data.getExpectedPayload();
        do {
            String payload = replay.getNext();
            if (null != payload) {
                if (null != expectedPayload) {
                    Assert.assertEquals(expectedPayload, payload);
                }
                count++;
            }
        } while (!replay.isEOD());
        long timeDiff = System.currentTimeMillis() - start;
        Assert.assertEquals(5, count);
        final long tolerance = 300; // due to jenkins
        final long expectedTime = data.getExpectedTime();
        Assert.assertTrue("replay time " + timeDiff + " not within tolerance ", 
            expectedTime - tolerance < timeDiff && timeDiff < expectedTime + tolerance);
        replay.disconnect();        
    }
    
    /**
     * Tests date-time timestamps with comma separator.
     */
    @Test
    public void testDateTimeCommaSeparator() {
        TestData data = REPLAY3;
        IReplaySource source = new FileSource(data.getFile());
        ReplayMechanism replay = new ReplayMechanism(source);
        assertReplay(replay, data);
    }

    /**
     * Tests date-time timestamps with comma separator (no-argument constructor).
     */
    @Test
    public void testDateTimeCommaSeparator2() {
        TestData data = REPLAY3;
        IReplaySource source = new FileSource(data.getFile());
        ReplayMechanism replay = new ReplayMechanism();
        replay.setSource(source);
        assertReplay(replay, data);
    }

    /**
     * Tests date-time timestamps with comma separator (full constructor).
     */
    @Test
    public void testDateTimeCommaSeparator3() {
        TestData data = REPLAY4;
        IReplaySource source = new FileSource(data.getFile());
        IDataManipulator manipulator = new IDataManipulator() {
            
            private DateTimeFormatter dtf = DateTimeFormat.forPattern("MM/dd/yyyy' 'HH:mm:ss");
            
            @Override
            public String composeData(long timestamp, String line) {
                String ar[] = line.split(",");
                DateTime symbolTimeStampNow = new DateTime(timestamp);
                String newDate[] = symbolTimeStampNow.toString(dtf).split(" ");
                return ar[0] + "," + newDate[0] + "," + newDate[1] + "," + ar[3] + "," + ar[4];
            }
            
            @Override
            public String changeInput(String line, boolean firstLine) {
                return firstLine ? line : line.replace((char) 65533, (char) 183);
            }
        };
        ReplayMechanism replay = new ReplayMechanism(source, manipulator, new DateTimeTimestampParser(1));
        replay.setSource(source);
        assertReplay(replay, data);
    }

}
