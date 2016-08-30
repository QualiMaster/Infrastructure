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
package backtype.storm.stateTransfer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a class (bolt, spout, delegate of those) has an explicit state to be 
 * transferred. In this case, annotate the class with {@link Stateful} and each individual field that shall
 * be part of the state with {@link PartOfState}. To be part of the transferrable state, the fields must not be
 * transient (even if kryo is used). If this annotation is not given on the class, each non-transient field is 
 * considered to be part of the state.
 */
@Target({ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface Stateful {
    
    /**
     * Whether non-transient attributes shall be included by default, even without {@link PartOfState} annotation.
     * 
     * @return whether non-transient attributes shall be included by default
     */
    public boolean considerAll() default true;
}
