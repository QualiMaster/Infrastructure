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
package tests.eu.qualimaster;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Abstract base test.
 * 
 * @author Holger Eichelberger
 */
public class AbstractTest {
    
    public static final File TESTDATA = determineTestDataDir();

    /**
     * Determines the {@code testdata} directory from {@code qm.base.dir} or from the file {@code testdata.dir}.
     * 
     * @return the testdata directory
     */
    private static File determineTestDataDir() {
        File result = new File(System.getProperty("qm.base.dir", "."), "testdata");
        if (!result.exists()) {
            File f = new File("testdata.dir");
            if (f.exists()) {
                try {
                    result = new File(new String(Files.readAllBytes(f.toPath())).trim());
                } catch (IOException e) {
                    System.out.println("Failed reading testdata.dir: " + e.getMessage());
                }
            }
        }
        return result;
    }
    
}
