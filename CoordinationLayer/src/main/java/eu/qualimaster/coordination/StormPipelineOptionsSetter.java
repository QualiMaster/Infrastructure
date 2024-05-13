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
package eu.qualimaster.coordination;

import java.io.Serializable;
import java.util.Map;

import eu.qualimaster.IOptionSetter;
import eu.qualimaster.infrastructure.PipelineOptions;

/**
 * A combined Storm config / pipeline options setter. Both parts can be changed by side effect.
 * 
 * @author Holger Eichelberger
 */
public class StormPipelineOptionsSetter implements IOptionSetter {

    @SuppressWarnings("rawtypes")
    private Map config;
    private PipelineOptions options;

    /**
     * Creates a storm pipeline options setter instance. The related Storm config is set by {@link #setConfig(Map)}.
     * 
     * @param options the options
     */
    public StormPipelineOptionsSetter(PipelineOptions options) {
        this(null, options);
    }

    /**
     * Creates a Storm pipeline options setter instance. The related Storm config is set by {@link #setConfig(Map)}.
     * 
     * @param config the Storm configuration
     * @param options the options
     */
    @SuppressWarnings("rawtypes")
    public StormPipelineOptionsSetter(Map config, PipelineOptions options) {
        this.config = config;
        this.options = options;
    }
    
    /**
     * Defines the Storm configuration.
     * 
     * @param config the configuration
     */
    @SuppressWarnings("rawtypes")
    public void setConfig(Map config) {
        this.config = config;
    }

    /**
     * Returns the Storm configuration.
     * 
     * @return the Storm configuration
     */
    @SuppressWarnings("rawtypes")
    public Map getConfig() {
        return config;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setOption(String key, Serializable value) {
        if (null != config) {
            config.put(key, value);
        }
        options.setOption(key, value);
    }
    
    /**
     * Turns pipeline options to a Storm configuration. If a Storm configuration was not given, a new instance
     * is created.
     * 
     * @see PipelineOptions#toConf(Map)
     */
    public void optionsToConf() {
        config = options.toConf(config);
    }

}
