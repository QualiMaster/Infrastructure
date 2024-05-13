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
package eu.qualimaster;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.LogManager;

/**
 * Reads the value of a configuration option from a properties file.
 *  
 * @param <T> the type of the value
 * @author Holger Eichelberger
 */
public interface PropertyReader<T> {

    static final PropertyReader<String> STRING_READER = new PropertyReader<String>() {

        @Override
        public String read(Properties properties, String key, String deflt) {
            return properties.getProperty(key, deflt);
        }
        
    };

    static final PropertyReader<Integer> INTEGER_READER = new PropertyReader<Integer>() {

        @Override
        public Integer read(Properties properties, String key, Integer deflt) {
            int result = deflt;
            try {
                Object val = properties.get(key);
                if (val instanceof Integer) {
                    result = ((Integer) val).intValue();
                } else if (null != val) {
                    result = Integer.parseInt(val.toString().trim());
                }
            } catch (NumberFormatException e) {
                // ignore - deflt is set anyway
            }
            return result;
        }
        
    };

    static final PropertyReader<Boolean> BOOLEAN_READER = new PropertyReader<Boolean>() {

        @Override
        public Boolean read(Properties properties, String key, Boolean deflt) {
            boolean result = deflt;
            Object val = properties.get(key);
            if (val instanceof Boolean) {
                result = ((Boolean) val).booleanValue();
            } else if (null != val) {
                result = Boolean.valueOf(val.toString().trim());
            }
            return result;
        }
        
    };
    
    static final PropertyReader<URL> URL_READER = new PropertyReader<URL>() {

        @Override
        public URL read(Properties properties, String key, URL deflt) {
            URL result = null;
            Object tmp = properties.get(key);
            if (tmp instanceof URL) {
                result = (URL) tmp;
            } else {
                try {
                    String url = properties.getProperty(key, deflt.toString());
                    if (url.length() > 0 && !url.endsWith("/")) {
                        url += "/";
                    }
                    result = new URL(url);
                } catch (MalformedURLException e) {
                    LogManager.getLogger(PropertyReader.class).error("Setting value for " + key + ":" + e.getMessage());
                }
            }
            return result;
        }
        
    };

    /**
     * Reads a value from <code>properties</code>.
     * 
     * @param properties the properties file
     * @param key the key to read
     * @param deflt the default value in case that key is not defined
     * @return the value or <code>deflt</code>
     */
    public T read(Properties properties, String key, T deflt);

}