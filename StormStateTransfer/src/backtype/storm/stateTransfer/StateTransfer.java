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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Performs the state transfer.
 * 
 * @author Holger Eichelberger
 */
public class StateTransfer {

    /**
     * Transfers the state from <code>state</code> to <code>target</code> using registered state transfer handlers.
     * 
     * @param target the target object
     * @param state the state object
     * @throws SecurityException in case that accessing <code>field</code> leads to a security problem
     * @throws IllegalArgumentException in case that accessing <code>field</code> happens with an illegal value
     * @throws IllegalAccessException in case that accessing <code>field</code> happens with an illegal access
     * @throws InstantiationException if an instance cannot be created
     */
    public static void transferState(Object target, Object state) throws SecurityException, 
        IllegalArgumentException, IllegalAccessException, InstantiationException {
        if (null != target) {
            Class<?> targetClass = target.getClass();
            if (targetClass.isInstance(state)) {
                transferState(targetClass.getFields(), target, state);
                transferState(targetClass.getDeclaredFields(), target, state);
            }
        }
    }

    /**
     * Transfers the state from <code>fields</code> of <code>state</code> to <code>target</code> using registered state 
     * transfer handlers.
     * 
     * @param fields the fields to process
     * @param target the target object
     * @param state the state object
     * @throws SecurityException in case that accessing <code>field</code> leads to a security problem
     * @throws IllegalArgumentException in case that accessing <code>field</code> happens with an illegal value
     * @throws IllegalAccessException in case that accessing <code>field</code> happens with an illegal access
     * @throws InstantiationException if an instance cannot be created
     */
    private static void transferState(Field[] fields, Object target, Object state) 
        throws SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        if (null != fields) {
            Class<?> targetClass = target.getClass();
            boolean hasExplicitState = targetClass.getAnnotation(Stateful.class) != null;
            for (int f = 0; f < fields.length; f++) {
                Field field = fields[f];
                PartOfState pos = field.getAnnotation(PartOfState.class);
                if (isPartOfState(field, hasExplicitState, pos)) {
                    boolean accessible = field.isAccessible();
                    if (!accessible) {
                        field.setAccessible(true);
                    }
                    StateTransferHandler<?> handler = StateTransferHandlerRegistry.getHandler(
                        target.getClass(), field);
                    Object stateValue = field.get(state);
                    Object targetValue = field.get(target);
                    boolean recurse = handler.transferState(pos, field, target, targetValue, stateValue);
                    if (recurse) {
                        transferState(targetValue, stateValue);
                    }
                    if (!accessible) {
                        field.setAccessible(false);
                    }
                }
            }
        }
    }
    
    /**
     * Returns whether <code>field</code> is considered to be part of the state.
     * 
     * @param field the field to query
     * @param hasExplicitState whether the containing class has an explicit state annotation
     * @param pos the part of state annotation of <code>field</code>
     * @return <code>true</code> if <code>field</code> is part of the state, <code>false</code> else
     */
    private static boolean isPartOfState(Field field, boolean hasExplicitState, PartOfState pos) {
        int modifiers = field.getModifiers();
        boolean isPart = !Modifier.isTransient(modifiers) && !Modifier.isFinal(modifiers);
        if (hasExplicitState) {
            isPart = pos != null;
        }
        return isPart;
    }

}
