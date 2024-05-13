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
package eu.qualimaster.coordination.profiling;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.easy.extension.QmConstants;
import net.ssehub.easy.varModel.model.datatypes.IDatatype;

/**
 * Implements helpers for serialization / deserialization of primitive data (not covered
 * by DML).
 * 
 * @author Holger Eichelberger
 */
class SerializationHelpers {

    /**
     * Defines a serialization helper (as primitive serializers may not be registered).
     * 
     * @param <T> the data type
     * @author Holger Eichelberger
     */
    interface ISerializationHelper<T extends Serializable> {

        /**
         * Returns the next element of type <code>T</code>.
         * 
         * @param input the input
         * @return the element
         * @throws IOException if reading the next element fails
         */
        public T next(IDataInput input) throws IOException;
        
    }

    static final ISerializationHelper<Integer> INT_HELPER = new ISerializationHelper<Integer>() {

        @Override
        public Integer next(IDataInput input) throws IOException {
            return input.nextInt();
        }
        
    };
    
    static final ISerializationHelper<String> STRING_HELPER = new ISerializationHelper<String>() {

        @Override
        public String next(IDataInput input) throws IOException {
            return input.nextString();
        }
        
    };

    static final ISerializationHelper<Boolean> BOOLEAN_HELPER = new ISerializationHelper<Boolean>() {

        @Override
        public Boolean next(IDataInput input) throws IOException {
            return input.nextBoolean();
        }
        
    };
    
    static final ISerializationHelper<Double> DOUBLE_HELPER = new ISerializationHelper<Double>() {

        @Override
        public Double next(IDataInput input) throws IOException {
            return input.nextDouble();
        }
        
    };

    private static final Map<String, ISerializationHelper<?>> HELPERS = new HashMap<String, ISerializationHelper<?>>();

    /**
     * Returns a serialization helper.
     * 
     * @param type the IVML type
     * @return the helper (may be <b>null</b>)
     */
    static ISerializationHelper<?> getHelper(IDatatype type) {
        return HELPERS.get(type.getName());
    }

    /**
     * Returns a serialization helper.
     * 
     * @param type the (IVML) type
     * @return the helper (may be <b>null</b>)
     */
    static ISerializationHelper<?> getHelper(String type) {
        return HELPERS.get(type);
    }
    
    static {
        HELPERS.put(QmConstants.TYPE_INTEGERPARAMETER, INT_HELPER);
        HELPERS.put(QmConstants.TYPE_STRINGPARAMETER, STRING_HELPER);
        HELPERS.put(QmConstants.TYPE_BOOLEANPARAMETER, BOOLEAN_HELPER);
        HELPERS.put(QmConstants.TYPE_REALPARAMETER, DOUBLE_HELPER);
        HELPERS.put(QmConstants.TYPE_LONGPARAMETER, INT_HELPER); // due to EASY
    }

}
