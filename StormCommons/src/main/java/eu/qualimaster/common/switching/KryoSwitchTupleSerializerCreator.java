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
package eu.qualimaster.common.switching;

import java.util.Map;

import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.base.serializer.KryoSwitchTupleSerializer;

/**
 * A default tuple serializer creator for kryo based on Storm configuration.
 * 
 * @author Holger Eichelberger
 */
public class KryoSwitchTupleSerializerCreator implements ISwitchTupleSerializerCreator {

    @SuppressWarnings("rawtypes")
    private Map conf;
    
    /**
     * Creates the default creator.
     * 
     * @param conf the storm configuration carrying the information about the available serializers
     */
    public KryoSwitchTupleSerializerCreator(@SuppressWarnings("rawtypes") Map conf) {
        this.conf = conf;
    }
    
    @Override
    public ISwitchTupleSerializer createSwitchTupleSerializer() {
        return new KryoSwitchTupleSerializer(conf);
    }

}
