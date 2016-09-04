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

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import eu.qualimaster.monitoring.profiling.Kalman;

/**
 * Some tests storing profiling data.
 * 
 * @author Holger Eichelberger
 */
public class StorageTests {

    /**
     * Tests storing.
     * 
     * @throws IOException shall not occur
     */
    @Test
    public void testKalman() throws IOException {
        File f = new File(FileUtils.getTempDirectory(), "kalman.tmp");
        f.delete();
        Kalman k1 = new Kalman();
        k1.store(f, "abba");
        
        Kalman k2 = new Kalman();
        k2.load(f, "abba");
    }
    
}
