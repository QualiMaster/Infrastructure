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
package eu.qualimaster.common.shedding;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.qualimaster.reflection.ReflectionHelper;

/**
 * A factory for registered load shedders.
 * 
 * @author Holger Eichelberger
 */
public class LoadShedderFactory {
    
    private static final Map<String, Class<? extends LoadShedder<?>>> INSTANCES 
        = new HashMap<String, Class<? extends LoadShedder<?>>>();
    
    static {
        // don't register the no shedder as it is default anyway
        register(DefaultLoadShedders.NTH_ITEM, NthItemShedder.class);
        register(DefaultLoadShedders.PROBABILISTIC, ProbabilisticShedder.class);
        register(DefaultLoadShedders.FAIR_PATTERN, FairPatternShedder100.class);
    }
    
    /**
     * Registers a shedder for <code>identifier</code> and the qualified name of <code>cls</code>.
     * 
     * @param identifier the identifier
     * @param cls the shedder class
     */
    public static void register(String identifier, Class<? extends LoadShedder<?>> cls) {
        if (null != identifier && null != cls) {
            INSTANCES.put(identifier, cls);
            INSTANCES.put(cls.getName(), cls);
        }
    }

    /**
     * Registers a shedder for <code>descriptor</code> and the qualified name of <code>cls</code>.
     * 
     * @param descriptor the descriptor
     * @param cls the shedder class
     */
    public static void register(ILoadShedderDescriptor descriptor, Class<? extends LoadShedder<?>> cls) {
        if (null != descriptor) {
            register(descriptor.getIdentifier(), cls);
            if (null != descriptor.getShortName() && null != cls) {
                INSTANCES.put(descriptor.getShortName(), cls);
            }
        }
    }

    /**
     * Identifies the shedder for <code>descriptor</code>.
     * 
     * @param descriptor the descriptor
     */
    public static void unregister(ILoadShedderDescriptor descriptor) {
        if (null != descriptor) {
            unregister(descriptor.getIdentifier());
        }
    }

    /**
     * Identifies the shedder for <code>identifier</code>.
     * 
     * @param identifier the identifier
     */
    public static void unregister(String identifier) {
        if (null != identifier) {
            Class<? extends LoadShedder<?>> cls = INSTANCES.get(identifier);
            if (null != cls) {
                INSTANCES.remove(cls.getName());
                INSTANCES.remove(identifier);
            }
        }
    }

    /**
     * Creates a shedder instance.
     * 
     * @param descriptor the shedder descriptor
     * @return the shedder, {@link NoShedder#INSTANCE} as default
     */
    public static LoadShedder<?> createShedder(ILoadShedderDescriptor descriptor) {
        LoadShedder<?> result = NoShedder.INSTANCE;
        if (null != descriptor) {
            result = createShedder(descriptor.getIdentifier());
        }
        return result;
    }

    /**
     * Creates a shedder instance.
     * 
     * @param identifier the identifier or class name to create the shedder for
     * @return the shedder, {@link NoShedder#INSTANCE} as default
     */
    public static LoadShedder<?> createShedder(String identifier) {
        LoadShedder<?> result = NoShedder.INSTANCE;
        if (null != identifier) {
            Class<? extends LoadShedder<?>> cls = INSTANCES.get(identifier);
            if (null != cls) {
                try {
                    result = ReflectionHelper.createInstance(cls);
                } catch (InstantiationException e) {
                    Logger.getLogger(LoadShedderFactory.class).error("Cannot create shedder instance: " 
                        + e.getMessage());
                } catch (IllegalAccessException e) {
                    Logger.getLogger(LoadShedderFactory.class).error("Cannot create shedder instance: " 
                        + e.getMessage());
                }
            }
        }
        return result;
    }
    
}
