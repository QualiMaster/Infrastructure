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
 * State transfer handler for directly copying instances.
 * 
 * @param <T> the type of the object
 * @author Holger Eichelberger
 */
public class ObjectValueStateTransferHandler<T> extends StateTransferHandler<T> {

    private boolean overrideAlways;
    
    /**
     * Creates the transfer handler for always overriding existing values.
     * 
     * @param cls the class to perform the transfer for
     */
    protected ObjectValueStateTransferHandler(Class<T> cls) {
        this(cls, true);
    }

    /**
     * Creates the transfer handler.
     * 
     * @param cls the class to perform the transfer for
     * @param overrideAlways if overriding any existing value shall be done, else if only the existing value is 
     *     <b>null</b>
     */
    protected ObjectValueStateTransferHandler(Class<T> cls, boolean overrideAlways) {
        super(cls);
        this.overrideAlways = overrideAlways;
    }

    @Override
    public boolean doStateTransfer(PartOfState annotation, Field field, Object target, T oldValue,
        T newValue) throws SecurityException, IllegalArgumentException, IllegalAccessException, 
        InstantiationException {
        if (overrideAlways || oldValue == null) {
            field.set(target, newValue);
        }
        return false;
    }

}
