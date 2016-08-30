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
import java.util.HashMap;
import java.util.Map;

/**
 * A registry for {@link StateTransferHandler state transfer handlers}.
 * 
 * @author Holger Eichelberger
 */
public class StateTransferHandlerRegistry {

    public static final StateTransferHandler<Object> DEFAULT_OBJECT_HANDLER 
        = new StateTransferHandler<Object>(Object.class) {
        
            @Override
            public boolean doStateTransfer(PartOfState annotation, Field field, Object target, Object oldValue, 
                Object newValue) throws SecurityException, IllegalArgumentException, IllegalAccessException, 
                InstantiationException {
                boolean recurse;
                if (null == oldValue || null == newValue) {
                    // if not present in target or no value, just set
                    field.set(target, newValue);
                    recurse = false;
                } else {
                    recurse = true;
                }
                return recurse;
            }
        };
        
    public static final StateTransferHandler<Object> DEFAULT_PRIMITIVE_HANDLER 
        = new StateTransferHandler<Object>(Object.class) {
        
            @Override
            public boolean doStateTransfer(PartOfState annotation, Field field, Object target, Object oldValue, 
                Object newValue) throws SecurityException, IllegalArgumentException, IllegalAccessException, 
                InstantiationException {
                if (null != newValue) { // cannot be, just be sure
                    field.set(target, newValue);
                }
                return true;
            }
        };
        
    
    private static Map<Class<?>, StateTransferHandler<?>> globalHandlers 
        = new HashMap<Class<?>, StateTransferHandler<?>>();
    private static Map<Class<?>, Map<Class<?>, StateTransferHandler<?>>> typeHandlers 
        = new HashMap<Class<?>, Map<Class<?>, StateTransferHandler<?>>>();
    
    static {
        registerHandler(new ListStateTransferHandler());
        registerHandler(new SetStateTransferHandler());
        registerHandler(new MapStateTransferHandler());
    }
    
    /**
     * Registers a global state transfer handler.
     * 
     * @param handler the handler (ignored if <b>null</b>)
     */
    public static void registerHandler(StateTransferHandler<?> handler) {
        registerHandler(null, handler);
    }

    /**
     * Registers a state transfer handler.
     * 
     * @param type the type to register the handler for (may be <b>null</b> for a global handler)
     * @param handler the handler (ignored if <b>null</b>)
     */
    public static void registerHandler(Class<?> type, StateTransferHandler<?> handler) {
        changeHandlers(type, true, handler);
    }

    /**
     * Unregisters a state transfer handler.
     * 
     * @param type the type to unregister the handler for (may be <b>null</b> for a global handler)
     * @param handler the handler (ignored if <b>null</b>)
     */
    public static void unregisterHandler(Class<?> type, StateTransferHandler<?> handler) {
        changeHandlers(type, false, handler);
    }

    /**
     * Unregisters a global state transfer handler.
     * 
     * @param handler the handler (ignored if <b>null</b>)
     */
    public static void unregisterHandler(StateTransferHandler<?> handler) {
        unregisterHandler(null, handler);
    }
    
    /**
     * Returns the state transfer handler responsible for the given class and field.
     * 
     * @param targetType the target type (may be a subclass of field's declaring class)
     * @param field the field to transfer the state into
     * @return the state handler (may be {{@link #DEFAULT_HANDLER} if no more specific one was found)
     */
    public static StateTransferHandler<?> getHandler(Class<?> targetType, Field field) {
        StateTransferHandler<?> result = null;
        Map<Class<?>, StateTransferHandler<?>> handlers = globalHandlers;
        if (null != targetType) {
            handlers = typeHandlers.get(targetType);
            if (null != handlers) {
                result = handlers.get(field.getType());
            }
        }
        if (null == result) {
            result = globalHandlers.get(field.getType());
        }
        if (null == result) {
            if (field.getType().isPrimitive()) {
                result = DEFAULT_PRIMITIVE_HANDLER;
            } else {
                result = DEFAULT_OBJECT_HANDLER;
            }
        }
        return result;
    }

    /**
     * Changes the handlers for <code>type</code>, either the type specific ones if defined or the global one by adding
     * or removing a handler.
     *  
     * @param type the type to look for
     * @param add add or remove <code>handler</code>
     * @param handler the handler to add/remove
     */
    private static void changeHandlers(Class<?> type, boolean add, StateTransferHandler<?> handler) {
        Map<Class<?>, StateTransferHandler<?>> handlers = globalHandlers;
        if (null != type) {
            handlers = typeHandlers.get(type);
            if (add && null == handlers) {
                handlers = new HashMap<Class<?>, StateTransferHandler<?>>();
                typeHandlers.put(type, handlers);
            }
        }
        if (null != handlers) {
            if (add) {
                handlers.put(handler.getType(), handler);
            } else {
                handlers.remove(handler.getType());
            }
        }
    }

}
