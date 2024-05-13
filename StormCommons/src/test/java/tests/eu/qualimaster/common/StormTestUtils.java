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

import java.util.HashMap;
import java.util.Map;

import backtype.storm.Config;
import backtype.storm.serialization.DefaultKryoFactory;

import com.esotericsoftware.kryo.Kryo;

/**
 * Utilities and helper methods for testing against Storm.
 * 
 * @author Holger Eichelberger
 */
public class StormTestUtils {

    /**
     * Creates a Storm-like configuration for Kryo.
     * 
     * @return the configuration instance
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static Map createStormKryoConf() {
        Map conf = new HashMap();
        conf.put(Config.TOPOLOGY_FALL_BACK_ON_JAVA_SERIALIZATION, Boolean.TRUE);
        conf.put(Config.TOPOLOGY_SKIP_MISSING_KRYO_REGISTRATIONS, Boolean.FALSE);
        conf.put(Config.TOPOLOGY_KRYO_FACTORY, "backtype.storm.serialization.DefaultKryoFactory");
        conf.put(Config.TOPOLOGY_TUPLE_SERIALIZER, "backtype.storm.serialization.types.ListDelegateSerializer");
        return conf;
    }

    /**
     * Creates a Storm-like Kryo instance. 
     * 
     * @return the Kryo instance
     */
    public static Kryo createStormKryo() {
        return createStormKryo(createStormKryoConf());
    }
    
    /**
     * Creates a Storm-like Kryo instance.
     *
     * @param conf the configuration map to create the kryo instance for
     * @return the Kryo instance
     */
    @SuppressWarnings({ "rawtypes" })
    public static Kryo createStormKryo(Map conf) {
        DefaultKryoFactory factory = new DefaultKryoFactory();
        Kryo k = factory.getKryo(conf);
        k.register(byte[].class);
        return k;
    }
    
}
