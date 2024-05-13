package eu.qualimaster.base.algorithm;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

import org.apache.storm.guava.collect.ImmutableMap;

/**
 * Utility functionality.
 * @author qin
 *
 */
public class AlgorithmUtils {
    /**
     * Maps primitive types to the wrapper class.
     */
    private static final Map<Class<?>, Class<?>> PRIMITIVES_TO_WRAPPERS
        = new ImmutableMap.Builder<Class<?>, Class<?>>()
            .put(boolean.class, Boolean.class)
            .put(byte.class, Byte.class)
            .put(char.class, Character.class)
            .put(double.class, Double.class)
            .put(float.class, Float.class)
            .put(int.class, Integer.class)
            .put(long.class, Long.class)
            .put(short.class, Short.class)
            .put(void.class, Void.class)
            .build();
  
    /**
     * Finds the constructor with specific types of arguments.
     * @param clazz the class to search
     * @param types the argument types
     * @return true if found, otherwise false
     */
    public static boolean findConstructor(Class<?> clazz, Class<?>... types) {
        boolean result = false;
        for (Constructor<?> constructor : clazz.getConstructors()) {
            Type[] parameterTypes = constructor.getGenericParameterTypes();        

            if (types.length == parameterTypes.length) {
                if (types.length == 0) { //no argument
                    result = true;
                } else {
                    for (int i = 0; i < types.length; i++) {
                        if (parameterTypes[i] instanceof ParameterizedType) {
                            ParameterizedType parameterizedArg = (ParameterizedType) parameterTypes[i];
                            //TODO:check also the actual type of the parameterized argument
        //                  Type[] typeArgs = parameterizedArg.getActualTypeArguments(); 
                            if (parameterizedArg.getRawType() == types[i]) {
                                result = true;
                            }
                        } else {
                            if (PRIMITIVES_TO_WRAPPERS.get(parameterTypes[i]) == types[i]) {
                                result = true;
                            }
                        }
                    }            
                } 
            }
        }
        return result;
    }
}
