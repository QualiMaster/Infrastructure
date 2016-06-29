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
package eu.qualimaster.dataManagement.accounts;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.LogManager;

import eu.qualimaster.dataManagement.DataManagementConfiguration;
import eu.qualimaster.dataManagement.DataManager;

/**
 * A keyword store for avoiding keywords and user names in the implementation / configuration.</br>
 * Example:
 * <code>
 *  PasswordEntry entry = PasswordStore.getEntry(vUser);
 *  account = entry.getUserName();
 *  password = entry.getPassword(); // handle null
 *  myValue = entry.getValue("myValue"); // handle null
 * </code>
 * 
 * @author Holger Eichelberger
 */
public class PasswordStore {
    
    public static final String SEPARATOR = ".";
    private static final String PASSWORD = "passwd";
    private static final String USER = "user";
    private static Properties properties = new Properties();
    
    /**
     * Represents all values assigned to a (virtual) user.
     * 
     * @author Holger Eichelberger
     */
    public static class PasswordEntry {

        private String vUser;
        
        /**
         * Creates a password entry instance.
         * 
         * @param vUser the virtual user mapping to more detailed information
         */
        private PasswordEntry(String vUser) {
            this.vUser = vUser;
        }
        
        /**
         * Returns a value assigned to vUser. 
         * 
         * @param key the generic ({@link #PASSWORD}, {@link #USER}) or algorithm-specific key
         * @return the value, may be <b>null</b> if undefined
         */
        public String getValue(String key) {
            return properties.getProperty(combineKey(vUser, key));
        }

        /**
         * Returns the vUser's password (if available).
         * 
         * @return the password of vUser, may be <b>null</b> if not specified
         */
        public String getPassword() {
            return getValue(PASSWORD);
        }

        /**
         * Returns the actual (mapped) user name.
         * 
         * @return the user name (may be {@link #vUser} if the vUser-specific value is not given in the accounts file)
         */
        public String getUserName() {
            String tmp = getValue(USER);
            return null == tmp ? vUser : tmp;
        }
        
        /**
         * Returns the original (unmapped) user name.
         * 
         * @return the original user name
         */
        public String getVUser() {
            return vUser;
        }

    }

    /**
     * Prevent external initialization.
     */
    private PasswordStore() {
    }
    
    // default init for infrastructure
    static {
        try {
            PasswordStore.load(new File(DataManagementConfiguration.getDfsPath(), "accounts.properties"));
        } catch (IOException e) {
            LogManager.getLogger(DataManager.class).warn("Reading the accounts file: " + e.getMessage());
        }
    }
    
    /**
     * Returns the password / information entry.
     * 
     * @param vUser the (virtual) user name
     * @return the password entry
     * @throws IllegalArgumentException in case that no <code>vUser</code> is given
     */
    public static PasswordEntry getEntry(String vUser) {
        if (null == vUser || vUser.isEmpty()) {
            throw new IllegalArgumentException("no vUser given");
        }
        return new PasswordEntry(vUser);
    }
    
    /**
     * Returns the combined key,
     * 
     * @param vUser the (virtual) user for grouping the entries
     * @param key the specific key within <code>vUser</code>
     * @return the combined key
     */
    private static String combineKey(String vUser, String key) {
        return vUser + SEPARATOR + key;
    }
    
    /**
     * Loads the underlying account data from a file.
     * 
     * @param file the file name to read from
     * @throws IOException in case of I/O problems
     */
    public static void load(File file) throws IOException {
        properties.clear();
        FileInputStream fis = new FileInputStream(file);
        properties.load(fis);
        fis.close();
    }

}
