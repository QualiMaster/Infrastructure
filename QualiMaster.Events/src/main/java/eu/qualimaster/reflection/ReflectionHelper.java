/*
 * Copyright 2009-2019 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.reflection;

/**
 * Some reflection helping/compatibility methods.
 * 
 * @author Holger Eichelberger
 */
public class ReflectionHelper {
    
    /**
     * Creates a new instance using the no-arg constructor of {@code cls}. This method wraps the deprecation
     * problem since Java 9/10 allowing a single code change when we switch there. So far, due to legacy issues,
     * we rely on Java 7.
     * 
     * @param <T> the type of object to create
     * @param cls the class to create the instance for
     * @return the instance
     * @throws IllegalAccessException in case that the constructor is not accessible
     * @throws InstantiationException in case that the instance cannot be created
     */
    @SuppressWarnings("deprecation")
    public static <T> T createInstance(Class<T> cls) throws IllegalAccessException, InstantiationException {
        return (T) cls.newInstance();
    }

}
