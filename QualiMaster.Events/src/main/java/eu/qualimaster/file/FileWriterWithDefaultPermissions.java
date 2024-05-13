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
package eu.qualimaster.file;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * A file writer setting the [@ink {@link FileOutputStreamWithDefaultPermissions#setDefaultPermissions(File) default
 * permissions} upon closing.
 * 
 * @author Holger Eichelberger
 */
public class FileWriterWithDefaultPermissions extends FileWriter {

    private File file;
    
    /**
     * Constructs a FileWriter object given a File object. Upon closing, the 
     * {@link FileOutputStreamWithDefaultPermissions#setDefaultPermissions(File) default permissions} will be set. 
     * [convenience]
     *
     * @param file a File object to write to.
     * @throws IOException  if the file exists but is a directory rather than
     *         a regular file, does not exist but cannot be created,
     *         or cannot be opened for any other reason
     */
    public FileWriterWithDefaultPermissions(File file) throws IOException {
        super(file);
        this.file = file;
    }

    /**
     * Constructs a FileWriter object given a File object. If the second
     * argument is <code>true</code>, then bytes will be written to the end
     * of the file rather than the beginning. Upon closing, the 
     * {@link FileOutputStreamWithDefaultPermissions#setDefaultPermissions(File) default permissions} will be set. 
     * [convenience]
     *
     * @param file  a File object to write to
     * @param append    if <code>true</code>, then bytes will be written
     *        to the end of the file rather than the beginning
     * @throws IOException  if the file exists but is a directory rather than
     *         a regular file, does not exist but cannot be created,
     *         or cannot be opened for any other reason
     */
    public FileWriterWithDefaultPermissions(File file, boolean append) throws IOException {
        super(file, true);
        this.file = file;
    }

    @Override
    public void close() throws IOException {
        super.close();
        FileOutputStreamWithDefaultPermissions.setDefaultPermissions(file);
    }
    
}
