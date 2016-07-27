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
package tests.eu.qualimaster.adaptation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.adaptation.events.AlgorithmConfigurationAdaptationEvent;
import eu.qualimaster.adaptation.events.ParameterConfigurationAdaptationEvent;
import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;

/**
 * A simple converter for implementation payload events intended to support debugging in QualiMaster pipelines.
 * 
 * @author Holger Eichelberger
 */
public class TriggerConverter {
    
    /**
     * A pipeline conversion entry.
     * 
     * @author Holger Eichelberger
     */
    public static class TranslationEntry {
        
        private String pipeline;
        private String pipelineElement;
        private String targetParameter;
        
        /**
         * Creates a new translation entry.
         * 
         * @param pipeline the pipeline name
         * @param pipelineElement the pipeline element name
         * @param targetParameter the target parameter name
         */
        public TranslationEntry(String pipeline, String pipelineElement, String targetParameter) {
            this.pipeline = pipeline;
            this.pipelineElement = pipelineElement;
            this.targetParameter = targetParameter;
        }
        
        /**
         * Returns the pipeline name.
         * 
         * @return the pipeline name
         */
        public String getPipeline() {
            return pipeline;
        }
        
        /**
         * Returns the pipeline element name.
         * 
         * @return the pipeline element name
         */
        public String getPipelineElement() {
            return pipelineElement;
        }
        
        /**
         * Returns the target parameter name.
         *  
         * @return the target parameter name
         */
        public String getTargetParameter() {
            return targetParameter;
        }
        
        /**
         * Returns whether this entry is valid.
         *  
         * @return <code>true</code> if it is valid, <code>false</code> else
         */
        private boolean isValid() {
            return null != pipeline && null != pipelineElement && null != targetParameter; 
        }
        
    }
    
    private static final List<TranslationEntry> ENTRIES = new ArrayList<TranslationEntry>();
    
    static {
        addEntry(new TranslationEntry("focusPip", "L3SNode", "keywords"));
    }
    
    /**
     * Adds a translation entry.
     * 
     * @param entry the translation entry
     */
    public static void addEntry(TranslationEntry entry) {
        if (null != entry && entry.isValid()) {
            ENTRIES.add(entry);
        }
    }
    
    /**
     * Implements the handling of {@link ParameterConfigurationAdaptationEvent}.
     * 
     * @author Holger Eichelberger
     */
    private static class ParameterConfigurationAdaptationHandler 
        extends EventHandler<ParameterConfigurationAdaptationEvent> {

        /**
         * Creates a handler instance.
         */
        protected ParameterConfigurationAdaptationHandler() {
            super(ParameterConfigurationAdaptationEvent.class);
        }

        @Override
        protected void handle(ParameterConfigurationAdaptationEvent event) {
            ParameterChangeCommand<Serializable> cmd = new ParameterChangeCommand<Serializable>(
                event.getPipeline(), event.getPipelineElement(), event.getParameter(), 
                event.getValue());
            cmd.execute();
        }
        
    }

    /**
     * Implements the handling of {@link AlgorithmConfigurationAdaptationEvent}.
     * 
     * @author Holger Eichelberger
     */
    private static class AlgorithmConfigurationAdaptationHandler 
        extends EventHandler<AlgorithmConfigurationAdaptationEvent> {

        /**
         * Creates a handler instance.
         */
        protected AlgorithmConfigurationAdaptationHandler() {
            super(AlgorithmConfigurationAdaptationEvent.class);
        }

        @Override
        protected void handle(AlgorithmConfigurationAdaptationEvent event) {
            AlgorithmChangeCommand cmd = new AlgorithmChangeCommand(event.getPipeline(), event.getPipelineElement(), 
                event.getAlgorithm());
            cmd.execute();
        }
        
    }

    /**
     * Runs the converter.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            
            @Override
            public void run() {
                EventManager.stop();
            }
        }));

        System.out.println("Starting event manager...");
        EventManager.register(new ParameterConfigurationAdaptationHandler());
        EventManager.register(new AlgorithmConfigurationAdaptationHandler());
        EventManager.start(false, true);
        System.out.println("Event manager started. Terminate by Ctrl-C.");
        
        // and wait
        new Thread(new Runnable() {
            
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }).start();
    }

}
