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
package eu.qualimaster.dataManagement.sources;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.ISerializer;
import eu.qualimaster.dataManagement.serialization.SerializerRegistry;
import eu.qualimaster.dataManagement.serialization.StringDataInput;

/**
 * Handles single and multiple source entries from generic sources.
 * 
 * @author Holger Eichelberger
 */
public class GenericMultiSourceHandler {
    
    private int tupleTypeCount;
    private Map<String, ConcurrentLinkedQueue<IDataInput>> queues;

    /**
     * Creates a source handler.
     * 
     * @param tupleTypeCount the number of tuples to be handled
     */
    public GenericMultiSourceHandler(int tupleTypeCount) {
        this.tupleTypeCount = tupleTypeCount;
        queues = new HashMap<String, ConcurrentLinkedQueue<IDataInput>>(tupleTypeCount);
    }

    /**
     * Turns a data input into the corresponding object.
     * 
     * @param cls the target class type
     * @param in the input data
     * @return the data object
     * @throws IOException in case that the object cannot be created
     */
    private <T> T toTuple(Class<T> cls, IDataInput in) throws IOException {
        ISerializer<T> serializer = SerializerRegistry.getSerializer(cls);
        if (null == serializer) {
            throw new IOException("No serializer for " + cls.getName() + " registered!");
        }
        return serializer.deserializeFrom(in);
    }
    
    /**
     * Queues <code>in</code> into the queue for <code>id</code>.
     * 
     * @param id the tuple id to queue for
     * @param in the input data
     */
    private void queue(String id, IDataInput in) {
        ConcurrentLinkedQueue<IDataInput> queue = queues.get(id);
        if (null == queue) {
            queue = new ConcurrentLinkedQueue<IDataInput>();
            queues.put(id, queue);
        }
        queue.offer(in);
    }

    /**
     * Polls the first entry from the queue for <code>id</code> and turns it into the corresponding object.
     * 
     * @param id the queue id
     * @param cls the target class type
     * @return the data object
     * @throws IOException in case that the object cannot be created
     */
    private <T> T poll(String id, Class<T> cls) throws IOException {
        T result;
        ConcurrentLinkedQueue<IDataInput> queue = queues.get(id);
        if (null == queue || queue.isEmpty()) {
            result = null;
        } else {
            result = toTuple(cls, queue.poll());
        }
        return result;
    }
    
    /**
     * Returns the next data tuple object.
     * 
     * @param tupleId the tuple id handled by the caller
     * @param cls the target class type
     * @param in the actual input data
     * @param fullLine whether the input data is the full line including the timestamp
     * @return the data tuple object
     * @throws IOException in case that the object cannot be created
     */
    public <T> T next(String tupleId, Class<T> cls, IDataInput in, boolean fullLine) throws IOException {
        T result;
        if (handlesMultiTupleTypes()) {
            // if multiple tuple types are defined on this source, queue the input data into the
            // queue given as first entry and obtain the next result for tupleId
            if (fullLine) {//if it's the full line, skip the timestamp
                in.nextLong();
            }
            String id = in.nextString();
            queue(id, in);
            result = poll(tupleId, cls);
        } else {
            result = toTuple(cls, in);
        }
        return result;
    }
    
    /**
     * Returns the tuple type count.
     * 
     * @return the tuple type count
     */
    public int getTupleTypeCount() {
        return tupleTypeCount;
    }
    
    /**
     * Returns whether this handler works on multi tuple types.
     * 
     * @return <code>true</code> for multi tuple types, <code>false</code> else
     */
    public boolean handlesMultiTupleTypes() {
        return tupleTypeCount > 1;
    }

    /**
     * Returns the next data tuple object for data given as String handled by {@link StringDataInput}. 
     * [convenience]
     * 
     * @param tupleId the tuple id handled by the caller
     * @param cls the target class type
     * @param data the data
     * @param separator the element separator within <code>data</code>
     * @return the data tuple object (<b>null</b> if no object can be created)
     */
    public <T> T next(String tupleId, Class<T> cls, String data, char separator) {
        return next(tupleId, cls, data, separator, false, false);
    }
    
    /**
     * Returns the next data tuple object for data given as String handled by {@link StringDataInput}. 
     * [convenience]
     * 
     * @param tupleId the tuple id handled by the caller
     * @param cls the target class type
     * @param data the data
     * @param separator the element separator within <code>data</code>
     * @param restAsString passes the rest of <code>data</code> except for the tuple identifier
     *   in case of multiple tuple to the first call (nextString expected)  
     * @param fullLine whether the input data is the full line including the timestamp
     * @return the data tuple object (<b>null</b> if no object can be created)
     */
    public <T> T next(String tupleId, Class<T> cls, String data, char separator, boolean restAsString, boolean fullLine) {
        T result;
        IDataInput in = nextDataInput(data, separator, restAsString);
        try {
            result = next(tupleId, cls, in, fullLine);
        } catch (IOException e) {
            Logger.getLogger(getClass()).error(e.getMessage() + " on input " + data);
            result = null;
        }
        return result;
    }
    
    /**
     * Creates a string data input.
     * @param data the data
     * @param separator the element separator within <code>data</code>
     * @param restAsString passes the rest of <code>data</code> except for the tuple identifier
     *   in case of multiple tuple to the first call (nextString expected) 
     * @return the string data input
     */
    public IDataInput nextDataInput(String data, char separator, boolean restAsString) {
        IDataInput in = null;
        if (restAsString) {
            in = new RestStringDataInput(data, separator, handlesMultiTupleTypes());
        } else {
            in = new StringDataInput(data, separator);
        }
        return in;
    }
    
    /**
     * Returns the timestamp in the data.
     * @param data the data
     * @param separator the element separator within <code>data</code>
     * @param restAsString passes the rest of <code>data</code> except for the tuple identifier
     *   in case of multiple tuple to the first call (nextString expected) 
     * @return the timestamp
     * @throws IOException the IO exception
     */
    public long nextTimestamp(String data, char separator, boolean restAsString) throws IOException {
        IDataInput in = nextDataInput(data, separator, restAsString);
        return in.nextLong();
    }
    
    /**
     * Returns the tuple id in the data.
     * @param data the data 
     * @param separator the element separator within <code>data</code>
     * @param restAsString passes the rest of <code>data</code> except for the tuple identifier
     *   in case of multiple tuple to the first call (nextString expected)
     * @return the tuple id
     * @throws IOException the IO exception
     */
    public String nextId(String data, char separator, boolean restAsString) throws IOException {
        IDataInput in = nextDataInput(data, separator, restAsString);
        in.nextLong();
        return in.nextString();
    }
    
    /**
     * A specific string data input returning only the first entry (if specified) else
     * the rest of the input. The rest is intended to be a string. 
     * 
     * @author Holger Eichelberger
     */
    private static class RestStringDataInput extends StringDataInput {

        private boolean separateFirst;
        
        /**
         * Creates the input instance.
         * 
         * @param data the data
         * @param delimiter the delimiter
         * @param separateFirst whether the first entry shall be handled as separate entry
         *      or as part of the rest
         */
        public RestStringDataInput(String data, char delimiter, boolean separateFirst) {
            super(data, delimiter);
            this.separateFirst = separateFirst;
        }
        
        @Override
        protected String next() throws IOException {
            String result;
            if (separateFirst && isBOD()) {
                result = super.next();
            } else {
                result = rest();
            }
            return result;
        }
        
    }

}