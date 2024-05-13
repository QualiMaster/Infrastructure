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
package eu.qualimaster.plugins;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.plugins.IPlugin.Action;

/**
 * Implements a plugin descriptor for a plugin implementing {@link IPlugin} which requires explicit loading..
 *  
 * @author Holger Eichelberger
 */
public class InterfacedPluginDescriptor extends BasicPluginDescriptor {

    private static final Logger LOGGER = LogManager.getLogger(InterfacedPluginDescriptor.class);
    private IPlugin instance;
    
    /**
     * Creates a basic plugin descriptor.
     * 
     * @param cls the plugin class
     * @param layers the layers
     * @throws PluginException in case that creating the plugin fails
     */
    InterfacedPluginDescriptor(Class<?> cls, ILayerDescriptor[] layers) throws PluginException {
        super(cls, layers);
        try {
            @SuppressWarnings("unchecked")
            Constructor<Object> constructor = (Constructor<Object>) cls.getConstructor();
            constructor.setAccessible(true); // instantiate in any case
            instance = (IPlugin) constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new PluginException("No-arg constructor not found for plugin " + cls.getName(), e);
        } catch (ClassCastException e) {
            throw new PluginException("Constructur was called with basic/no plugin class", e);
        } catch (InstantiationException e) {
            throw new PluginException("Cannot instantiate plugin " + cls.getName() + ": " + e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new PluginException("Cannot access plugin constructor " + cls.getName() + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new PluginException("Cannot instantiate plugin " + cls.getName() + ": " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new PluginException("Cannot instantiate plugin " + cls.getName() + ": " + e.getMessage(), e);
        }
    }

    @Override
    public ILayerDescriptor assignedTo(Action action) {
        ILayerDescriptor result = super.assignedTo(action);
        if (null == result) {
            try {
                Method method = getPluginClass().getMethod("assignedTo", Action.class);
                result = (ILayerDescriptor) method.invoke(instance, action);
            } catch (ClassCastException e) {
                LOGGER.warn("assignedTo on " + getClassName(), e);
            } catch (NoSuchMethodException e) {
                LOGGER.warn("assignedTo on " + getClassName(), e);
            } catch (SecurityException e) {
                LOGGER.warn("assignedTo on " + getClassName(), e);
            } catch (IllegalAccessException e) {
                LOGGER.warn("assignedTo on " + getClassName(), e);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("assignedTo on " + getClassName(), e);
            } catch (InvocationTargetException e) {
                LOGGER.warn("assignedTo on " + getClassName(), e);
            }
        }
        return result;
    }

    
    @Override
    public void execute(Action action) throws PluginException {
        try {
            Method method = getPluginClass().getMethod("execute", Action.class);
            method.invoke(instance, action);
            if (Action.START == action) {
                setState(State.STARTED);
            } else if (Action.SHUTDOWN == action) {
                setState(State.SHUTDOWN);
            }

        } catch (NoSuchMethodException e) {
            throw new PluginException("Cannot execute " + action + " as method is missing on " + getClassName(), e);
        } catch (IllegalAccessException e) {
            throw new PluginException("Cannot execute " + action + " on " + getClassName() + ": " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new PluginException("Cannot execute " + action + " on " + getClassName() + ": " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            throw new PluginException("Cannot execute " + action + " on " + getClassName() + ": " + e.getMessage(), e);
        }
    }

}
