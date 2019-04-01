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

import java.util.Arrays;

import eu.qualimaster.plugins.IPlugin.Action;

/**
 * Implements a basic plugin descriptor assuming that the plugin is initialized upon class loading time.
 *  
 * @author Holger Eichelberger
 */
class BasicPluginDescriptor implements IPluginDescriptor {

    private Class<?> cls;
    private ILayerDescriptor[] layers;
    private State state = State.UNKNOWN;
    
    /**
     * Creates a basic plugin descriptor.
     * 
     * @param cls the plugin class
     * @param layers the layers
     * @throws PluginException in case that creating the plugin fails
     */
    BasicPluginDescriptor(Class<?> cls, ILayerDescriptor[] layers) throws PluginException {
        this.cls = cls;
        this.layers = layers;
        this.state = State.INITIALIZED;
    }
    
    @Override
    public final String getClassName() {
        return cls.getName();
    }
    
    /**
     * Returns the plugin class.
     * 
     * @return the class
     */
    protected Class<?> getPluginClass() {
        return cls;
    }

    @Override
    public ILayerDescriptor assignedTo(Action action) {
        return action.ordinal() < layers.length ? layers[action.ordinal()] : null;
    }

    @Override
    public void execute(Action action) throws PluginException {
        // no plugin actions to be executed
        if (Action.SHUTDOWN == action) {
            this.state = State.STOPPED;
        }
    }
    
    /**
     * Changes the state.
     * 
     * @param state the new state
     */
    protected void setState(State state) {
        this.state = state;
    }

    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public String toString() {
        return getClassName() + " " + Arrays.toString(layers) + " " + state;
    }

}
