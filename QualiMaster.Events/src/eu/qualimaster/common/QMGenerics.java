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
package eu.qualimaster.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows detailing the generic type parameters of a field or a method and its return type to guideline the 
 * VIL type system. Currently, <code>java.util.Map</code>, <code>java.util.List</code> and <code>java.util.Set</code> 
 * are also mapped into respective VIL instances and back.
 * 
 * @author Holger Eichelberger
 */
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface QMGenerics {

    /**
     * The generics.
     * 
     * @return the generics
     */
    public Class<?>[] types();
    
}
