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
package eu.qualimaster.dataManagement.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Seralizes an object from external.
 * 
 * @param <T> the type to be serialized
 * @author Holger Eichelberger
 * @author Cui Qin
 */
public interface ISerializer <T> {
    
    /**
     * Serializes <code>object</code> to <code>out</code>. 
     * 
     * @param object the object to be serialized (may be <b>null</b>)
     * @param out the output stream to serialize to
     * @throws IOException in case that <code>object</code> cannot be serialized
     */
    public void serializeTo(T object, OutputStream out) throws IOException;
    
    /**
     * Deserializes an object handled by this serializer from <code>in</code>. 
     * 
     * @param in the input stream to serialize from
     * @return the deserialized object (or <b>null</b>)
     * @throws IOException in case that <code>object</code> cannot be deserialized
     */
    public T deserializeFrom(InputStream in) throws IOException;
    
    /**
     * Serializes <code>object</code> to <code>out</code>. 
     * 
     * @param object the object to be serialized (may be <b>null</b>)
     * @param out the output stream to serialize to
     * @throws IOException in case that <code>object</code> cannot be serialized
     */
    public void serializeTo(T object, IDataOutput out) throws IOException;
    
    /**
     * Deserializes an object handled by this serializer from <code>in</code>. 
     * 
     * @param in the input stream to serialize from
     * @return the deserialized object (or <b>null</b>)
     * @throws IOException in case that <code>object</code> cannot be deserialized
     */
    public T deserializeFrom(IDataInput in) throws IOException; 

}
