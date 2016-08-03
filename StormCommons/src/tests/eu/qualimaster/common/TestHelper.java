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
package tests.eu.qualimaster.common;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Test utility functions.
 * 
 * @author Holger Eichelberger
 */
public class TestHelper {
    
    public static final int LOCAL_ZOOKEEPER_PORT = 2000;

    /**
     * Tracks the files / folders or deletes untracked files or folders in the temp directory.
     * This method may help getting rid of logs created but not deleted by the Storm local cluster.
     * 
     * @param contents the contents of the temp dir (top-level only)
     * @param delete if <code>false</code> collect the files, else if <code>true</code> delete undeleted
     * @return <code>contents</code> or a new instance created if <code>contents</code> is <b>null</b>
     */
    public static Set<File> trackTemp(Set<File> contents, boolean delete) {
        if (null == contents) {
            contents = new HashSet<File>();
        }
        String tmp = System.getProperty("java.io.tmpdir", null);
        if (null != tmp) {
            File tmpDir = new File(tmp);
            File[] files = tmpDir.listFiles();
            if (null != files) {
                for (int f = 0; f < files.length; f++) {
                    File file = files[f];
                    if (delete) {
                        if (!contents.contains(file)) {
                            file.delete();
                        }
                    } else {
                        contents.add(file);
                    }
                }
            }
        }
        return contents;
    }
    
}
