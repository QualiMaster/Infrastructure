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
package eu.qualimaster.events;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import eu.qualimaster.common.QMInternal;

/**
 * The most basic abstract event implementation.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractEvent implements IEvent {

    @QMInternal
    public static final String ATTRIBUTE_NAME_SEPARATOR = ":";
    private static final long serialVersionUID = -4876864528903813015L;

    @QMInternal
    @Override
    public String getChannel() {
        return null;
    }
    
    @QMInternal
    @Override
    public String toString() {
        return toString(this);
    }
    
    /**
     * Turns an event generically into a string.
     * 
     * @param event the event
     * @return the string representation
     */
    @QMInternal
    public static String toString(IEvent event) {
        Class<?> cls = event.getClass();
        String result = cls.getSimpleName() + " ";
        
        // getFields would also be ok, but we want to print out the most specific fields first
        Class<?> printCls = cls;
        while (null != printCls) {
            Field[] fields = printCls.getDeclaredFields();
            for (int f = 0; f < fields.length; f++) {
                Field field = fields[f];
                if (!Modifier.isStatic(field.getModifiers())) { // not constants, serialVersionUid
                    try {
                        field.setAccessible(true);
                        result += field.getName() + ATTRIBUTE_NAME_SEPARATOR + " " + field.get(event) + " ";
                    } catch (IllegalAccessException | ExceptionInInitializerError | NullPointerException 
                        | IllegalArgumentException e) {
                        // ignroe
                    }
                }
            }
            printCls = printCls.getSuperclass();
        }
        
        return result;
    }

}
