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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.LogManager;

/**
 * A registry for (external) serializers. Automatically registers default built-in serializers for
 * the basic Java types.
 *
 * @author Holger Eichelberger, Cui Qin
 */
public class SerializerRegistry {

    private static Map<String, ISerializer<?>> SERIALIZERS = new HashMap<String, ISerializer<?>>();

    /**
     * Returns a serializer for <code>cls</code>.
     *
     * @param <T> the object type
     * @param cls the class to return the serializer for
     * @return the serializer or <b>null</b> of none was found
     */
    public static synchronized <T> ISerializer<T> getSerializer(Class<T> cls) {
    	//LogManager.getLogger(SerializerRegistry.class).info(
         //       "get serializer instance: cls.getName() = "+ cls.getSimpleName());
        return getSerializer(cls.getName(), cls);    	
    }
    
    /**
     * Returns a serializer for <code>cls</code>.
     *
     * @param <T> the object type
     * @param clsName the class name
     * @param cls the class to return the serializer for
     * @return the serializer or <b>null</b> of none was found
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> ISerializer<T> getSerializer(String clsName, Class<T> cls) {
        ISerializer<T> result;
        if (null == clsName) {
            result = null;
           // LogManager.getLogger(SerializerRegistry.class).info(
            //        "clsName is null");
        } else {        	
            result = (ISerializer<T>) SERIALIZERS.get(clsName);
            if(null == result){
            	 LogManager.getLogger(SerializerRegistry.class).info(
                         "SERIALIZERS does not contain clsName = "+ clsName);
            	 //LogManager.getLogger(SerializerRegistry.class).info(
                  //       "SERIALIZERS size  = "+ SERIALIZERS.size());
            }
        }
        return result;
    }
  
    /**
     * Returns a safe serializer returning an empty serializer if none is registered.
     * 
     * @param <T> the object type
     * @param clsName the class name
     * @param cls the return class type
     * @return the serializer
     */
    public static synchronized <T> ISerializer<T> getSerializerSafe(String clsName, Class<T> cls) {
        ISerializer<T> result = getSerializer(clsName, cls);
        if (null == result) {
            result = new EmptyDefaultSerializers.EmptyDefaultBasicTypeSerializer<T>();
        }
        return result;
    }

    /**
     * Returns a list serializer.
     * 
     * @param <T> the element type
     * @param clsName the class name
     * @param cls the return class type
     * @return the list serializer serializer (<b>null</b> if none is registered)
     */
    @SuppressWarnings("unchecked")
    public static synchronized <T> ISerializer<List<T>> getListSerializer(String clsName, Class<T> cls) {
        ISerializer<List<T>> result;
        if (null == cls) {
    	    result = null;
    	} else {
    	    result = (ISerializer<List<T>>) SERIALIZERS.get(clsName);
    	}
        return result;
    }

    /**
     * Returns a list serializer and if none is registered an empty list serializer.
     * 
     * @param <T> the element type
     * @param clsName the class name
     * @param cls the return class type
     * @return the list serializer serializer
     */
    public static synchronized <T> ISerializer<List<T>> getListSerializerSafe(String clsName, Class<T> cls) {
        ISerializer<List<T>> result = getListSerializer(clsName, cls);
        if (null == result) {
            result = new EmptyDefaultSerializers.EmptyDefaultListTypeSerializer<T>();
        }
        return result;
    }

    /**
     * Registers a serializer class for a given class name.
     *
     * @param cls the class to register the serializer for
     * @param serializer the serializer instance
     * @return <code>true</code> for successful, <code>false</code> else
     */
    public static synchronized <T> boolean register(Class<T> cls, Class<? extends ISerializer<T>> serializer) {
        return register(cls.getName(), serializer);
    }
    
    /**
     * Registers a serializer class for a given class name.
     *
     * @param clsName the class name to register the serializer for
     * @param serializer the serializer instance
     * @return <code>true</code> for successful, <code>false</code> else
     */
    public static synchronized <T> boolean register(String cls, Class<? extends ISerializer<T>> serializer) {
        boolean successful;
        try {
            successful = register(cls, serializer.newInstance());
            //LogManager.getLogger(SerializerRegistry.class).info(
            //        "register serializer successfully: cls = "+ cls+ "SERIALIZERS size = "+ SERIALIZERS.size());
        } catch (InstantiationException e) {
            LogManager.getLogger(SerializerRegistry.class).error(
                "Cannot create serializer instance: " + e.getMessage());
            successful = false;
        } catch (IllegalAccessException e) {
            LogManager.getLogger(SerializerRegistry.class).error(
                "Cannot create serializer instance: " + e.getMessage());
            successful = false;
        }
        return successful;
    }

    /**
     * Registers a serializer for a given class name.
     *
     * @param cls the class name to register the serializer for
     * @param serializer the serializer instance
     * @return <code>true</code> for successful, <code>false</code> else
     */
    public static synchronized <T> boolean register(Class<T> cls, ISerializer<T> serializer) {
        return register(cls.getName(), serializer);
    }
    
    /**
     * Registers a serializer for a given class name.
     *
     * @param clsName the class name to unregister the serializer
     * @param serializer the serializer instance
     * @return <code>true</code> for successful, <code>false</code> else
     */
    public static synchronized <T> boolean register(String cls, ISerializer<T> serializer) {
        boolean successful = false;
        if (null != serializer) {
            if (null != cls) {
                ISerializer<?> registered = SERIALIZERS.get(cls);
                if (null == registered) {
                    SERIALIZERS.put(cls, serializer);
                }
                successful = true;
            }
        }
        return successful;
    }

    /**
     * Unregisters a serializer for a given class.
     *
     * @param cls the class name to unregister the serializer for
     * @return <code>true</code> for successful, <code>false</code> else
     */
    public static synchronized boolean unregister(Class<?> cls) {
        return unregister(cls.getName());
    }
    
    /**
     * Unregisters a serializer for a given class name.
     *
     * @param clsName the class name to unregister the serializer for
     * @return <code>true</code> for successful, <code>false</code> else
     */
    public static synchronized boolean unregister(String clsName) {
        boolean successful = false;
        if (null != clsName) {
            ISerializer<?> serializer = SERIALIZERS.get(clsName);
            if (null == serializer) {
                successful = false;
            } else {
                successful = null != SERIALIZERS.remove(clsName);
            }
        }
        return successful;
      }
}
