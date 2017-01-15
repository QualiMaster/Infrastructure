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
package tests.eu.qualimaster.monitoring;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.profiling.ProfileReader;
import eu.qualimaster.monitoring.tracing.TraceReader.PipelineEntry;
import tests.eu.qualimaster.coordination.Utils;

/**
 * Tests for the profile reader.
 * 
 * @author Holger Eichelberger
 */
public class ProfileReaderTest {
    
    /**
     * Simple test for the profile reader.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testProfileReader() throws IOException {
        File input = new File(Utils.getTestdataDir(), "infraLog.csv");
        File outputFolder = new File(FileUtils.getTempDirectory(), "profileReader");
        FileUtils.deleteQuietly(outputFolder);
        outputFolder.mkdirs();
        List<PipelineEntry> pips = ProfileReader.readBackProfile(input, outputFolder, "alg", null);
        Assert.assertTrue(null != pips && !pips.isEmpty());
        File[] inOutput = outputFolder.listFiles();
        Assert.assertTrue(null != inOutput && inOutput.length > 0);
        FileUtils.deleteQuietly(outputFolder);
    }

}
