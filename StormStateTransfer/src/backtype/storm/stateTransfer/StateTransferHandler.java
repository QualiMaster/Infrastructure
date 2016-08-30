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

/**
 * The interface of a pluggable state transfer handler. Although an object type is not needed for all kinds of handlers,
 * it allows writing specific handlers more easily hopefully reducing the needs for casts.
 *
 * @param <T> the field type handled by this handler
 * @author Holger Eichelberger
 */
public abstract class StateTransferHandler<T> {

    private Class<T> type;
    
    /**
     * Creates a state transfer handler.
     * 
     * @param type the field type the handler is handling
     */
    protected StateTransferHandler(Class<T> type) {
        this.type = type;
    }
    
    /**
     * Performs the state transfer from <code>source</code> to <code>target</code>.
     * 
     * @param annotation the part-of-state annotation of the field (may be <b>null</b>)
     * @param field the field to set the value for
     * @param target the target object to set <code>value</code> on
     * @param oldValue the current value of <code>field</code>
     * @param newValue the new value that shall be set
     * @return <code>true</code> for recurse on the fields of <code>field</code>, <code>false</code> for stop recursion
     * @throws SecurityException in case that accessing <code>field</code> leads to a security problem
     * @throws IllegalArgumentException in case that accessing <code>field</code> happens with an illegal value
     * @throws IllegalAccessException in case that accessing <code>field</code> happens with an illegal access
     * @throws InstantiationException if an instance of <code>T</code> cannot be created
     */
    public abstract boolean doStateTransfer(PartOfState annotation, Field field, Object target, T oldValue, T newValue) 
        throws SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException;
    
    /**
     * Performs the state transfer from <code>source</code> to <code>target</code>. <code>oldValue</code> and 
     * <code>newValue</code> must be compatible with <code>T</code>.
     * 
     * @param annotation the part-of-state annotation of the field (may be <b>null</b>)
     * @param field the field to set the value for
     * @param target the target object to set <code>value</code> on
     * @param oldValue the current value of <code>field</code>
     * @param newValue the new value that shall be set
     * @return <code>true</code> for recurse on the fields of <code>field</code>, <code>false</code> for stop recursion
     * @throws SecurityException in case that accessing <code>field</code> leads to a security problem
     * @throws IllegalArgumentException in case that accessing <code>field</code> happens with an illegal value
     * @throws IllegalAccessException in case that accessing <code>field</code> happens with an illegal access
     * @throws InstantiationException if an instance of <code>T</code> cannot be created
     */
    public boolean transferState(PartOfState annotation, Field field, Object target, Object oldValue, Object newValue) 
        throws SecurityException, IllegalArgumentException, IllegalAccessException, InstantiationException {
        return doStateTransfer(annotation, field, target, type.cast(oldValue), type.cast(newValue));
    }
    
    /**
     * Returns the actual type handled by this handler.
     * 
     * @return the type
     */
    public Class<T> getType() {
        return type;
    }
    
    /**
     * Returns the state handling strategy.
     * 
     * @param annotation the annotation (may be <b>null</b>)
     * @return the strategy, {@link StateHandlingStrategy#DEFAULT} if no annotation is given
     */
    protected static StateHandlingStrategy getStrategy(PartOfState annotation) {
        return null == annotation ? StateHandlingStrategy.DEFAULT : annotation.strategy();
    }

    /**
     * Performs default state transfer from <code>source</code> to <code>target</code>.
     * 
     * @param annotation the part-of-state annotation of the field (may be <b>null</b>)
     * @param field the field to set the value for
     * @param target the target object to set <code>value</code> on
     * @param oldValue the current value of <code>field</code>
     * @param newValue the new value that shall be set
     * @return <code>true</code> for recurse on the fields of <code>field</code>, <code>false</code> for stop recursion
     * @throws SecurityException in case that accessing <code>field</code> leads to a security problem
     * @throws IllegalArgumentException in case that accessing <code>field</code> happens with an illegal value
     * @throws IllegalAccessException in case that accessing <code>field</code> happens with an illegal access
     * @throws InstantiationException if an instance of <code>T</code> cannot be created
     */
    protected static boolean doDefaultObjectStateTransfer(PartOfState annotation, Field field, Object target, 
        Object oldValue, Object newValue) throws SecurityException, IllegalArgumentException, IllegalAccessException, 
        InstantiationException {
        return StateTransferHandlerRegistry.DEFAULT_OBJECT_HANDLER.doStateTransfer(
            annotation, field, target, oldValue, newValue);        
    }
    
}
