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
package tests.eu.qualimaster.coordination;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;
import eu.qualimaster.dataManagement.serialization.SerializerRegistry;

/**
 * A specific integer serializer as there is currently no default one.
 * 
 * @author Holger Eichelberger
 */
public class IntSerializer implements ISerializer<Integer> {

    @Override
    public void serializeTo(Integer object, OutputStream out) throws IOException {
    }

    @Override
    public Integer deserializeFrom(InputStream in) throws IOException {
        return null;
    }

    @Override
    public void serializeTo(Integer object, IDataOutput out) throws IOException {
        out.writeInt(object.intValue());
    }

    @Override
    public Integer deserializeFrom(IDataInput in) throws IOException {
        return in.nextInt();
    }
    
    /**
     * Registers this serializer if needed.
     * 
     * @return <code>true</code> if a new serializer was registered, <code>false</code> else
     */
    public static boolean registerIfNeeded() {
        boolean localSer = SerializerRegistry.getSerializer(Integer.class) == null;
        if (localSer) {
            SerializerRegistry.register(Integer.class, new IntSerializer());
        }
        return localSer;
    }
    
    /**
     * Unregisters this serializer.
     * 
     * @param registered the result of {@link #registerIfNeeded()}
     */
    public static void unregisterIfNeeded(boolean registered) {
        if (registered) {
            SerializerRegistry.unregister(Integer.class);
        }
    }
    
}