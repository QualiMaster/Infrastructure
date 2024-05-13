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
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.coordination.commands.AlgorithmChangeCommand;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.commands.LoadSheddingCommand;
import eu.qualimaster.coordination.commands.ParameterChangeCommand;
import eu.qualimaster.monitoring.events.AbstractPipelineElementMonitoringEvent;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.IEnactmentCompletedMonitoringEvent;
import eu.qualimaster.monitoring.events.LoadSheddingChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.ParameterChangedMonitoringEvent;
import eu.qualimaster.monitoring.events.ReplayChangedMonitoringEvent;

/**
 * Provides comparable signatures of events and commands used during enactment. The aim is to
 * derive signatures that are used to identify the causing command with the respective enactment 
 * completed event. Not all commands / events must have a signature, i.e., commands / events without
 * signature shall lead to an immediate {@link CoordinationCommandExecutionEvent}.
 * 
 * @author Holger Eichelberger
 */
public class EnactmentSignatureProvider {

    /**
     * Defines a signature provider.
     * 
     * @param <T> the type to provide the signature for
     * @author Holger Eichelberger
     */
    public abstract static class SignatureProvider<T> {
        
        private Class<T> cls;
        
        /**
         * Creates a signature provider instance.
         * 
         * @param cls the handled class
         */
        protected SignatureProvider(Class<T> cls) {
            this.cls = cls;
        }

        /**
         * Returns the signature for <code>obj</code>.
         * 
         * @param obj the object to return the signature for
         * @return the signature (may be <b>null</b>)
         */
        public abstract String getSignature(T obj);

        /**
         * Returns the signature for <code>obj</code>.
         * 
         * @param obj the object to return the signature for
         * @return the signature (may be <b>null</b> if there is no signature)
         */
        private String doSignature(Object obj) {
            return null == obj ? null : getSignature(cls.cast(obj));
        }
        
        /**
         * Returns the class that is handled by this provider.
         * 
         * @return the class
         */
        public Class<T> handles() {
            return cls;
        }
        
    }
    
    private static final Map<Class<?>, SignatureProvider<?>> PROVIDERS = new HashMap<Class<?>, SignatureProvider<?>>();

    static {
        registerAlgorithmChangeProviders();
        registerParameterChangeProviders();
        registerLoadSheddingChangeProviders();
        registerReplayChangeProviders();
        // wavefront -> collector
        // monitoring change -> collector
    }
    
    /**
     * Returns a mapped back element name from an implementation name.
     * 
     * @param evt the pipeline element monitoring event
     * @return the (mapped back) element name
     */
    private static String getPipelineElementName(AbstractPipelineElementMonitoringEvent evt) {
        String eltName = evt.getPipelineElement();
        INameMapping mapping = CoordinationManager.getNameMapping(evt.getPipeline());
        String tmp = mapping.getPipelineNodeByImplName(eltName);
        if (null != tmp) {
            eltName = tmp;
        }
        return eltName;
    }
    
    /**
     * Registers the provider instances for algorithm change signatures.
     */
    private static void registerAlgorithmChangeProviders() {
        registerProvider(new SignatureProvider<AlgorithmChangedMonitoringEvent>(AlgorithmChangedMonitoringEvent.class) {

            @Override
            public String getSignature(AlgorithmChangedMonitoringEvent evt) {
                return getAlgChangeSignature(evt.getPipeline(), getPipelineElementName(evt), evt.getAlgorithm());
            }
            
        });
        registerProvider(new SignatureProvider<AlgorithmChangeCommand>(AlgorithmChangeCommand.class) {

            @Override
            public String getSignature(AlgorithmChangeCommand cmd) {
                String result = null;
                if (CoordinationConfiguration.doCommandCompletionOnEvent()) {
                    result = getAlgChangeSignature(cmd.getPipeline(), cmd.getPipelineElement(), cmd.getAlgorithm()); 
                } else {
                    result = null;
                }
                return result;
            }
            
        });
    }

    /**
     * Registers the provider instances for replay change signatures.
     */
    private static void registerLoadSheddingChangeProviders() {
        registerProvider(new SignatureProvider<LoadSheddingChangedMonitoringEvent>(
            LoadSheddingChangedMonitoringEvent.class) {

            @Override
            public String getSignature(LoadSheddingChangedMonitoringEvent evt) {
                return getLoadSheddingChangeSignature(evt.getPipeline(), getPipelineElementName(evt), 
                    evt.getShedder());
            }
            
        });
        registerProvider(new SignatureProvider<LoadSheddingCommand>(LoadSheddingCommand.class) {

            @Override
            public String getSignature(LoadSheddingCommand cmd) {
                return getLoadSheddingChangeSignature(cmd.getPipeline(), cmd.getPipelineElement(), 
                    cmd.getShedder());
            }
            
        });
    }
    
    /**
     * Registers the provider instances for replay change signatures.
     */
    private static void registerReplayChangeProviders() {
        registerProvider(new SignatureProvider<ReplayChangedMonitoringEvent>(ReplayChangedMonitoringEvent.class) {

            @Override
            public String getSignature(ReplayChangedMonitoringEvent evt) {
                return getReplayChangeSignature(evt.getPipeline(), getPipelineElementName(evt), 
                    evt.getTicket(), evt.getStartReplay());
            }
            
        });
        registerProvider(new SignatureProvider<ReplayChangedMonitoringEvent>(ReplayChangedMonitoringEvent.class) {

            @Override
            public String getSignature(ReplayChangedMonitoringEvent cmd) {
                return getReplayChangeSignature(cmd.getPipeline(), cmd.getPipelineElement(), 
                    cmd.getTicket(), cmd.getStartReplay());
            }
            
        });
    }


    /**
     * Registers the provider instances for parameter change signatures.
     */
    @SuppressWarnings("rawtypes")
    private static void registerParameterChangeProviders() {
        registerProvider(new SignatureProvider<ParameterChangedMonitoringEvent>(ParameterChangedMonitoringEvent.class) {

            @Override
            public String getSignature(ParameterChangedMonitoringEvent evt) {
                String elt = evt.getPipelineElement();
                INameMapping mapping = CoordinationManager.getNameMapping(evt.getPipeline());
                if (null != mapping) {
                    elt = mapping.getParameterBackMapping(evt.getPipelineElement(), evt.getParameter());
                }
                return getParamChangeSignature(evt.getPipeline(), elt, evt.getParameter(), evt.getValue());
            }
            
        });
        registerProvider(new SignatureProvider<ParameterChangeCommand>(ParameterChangeCommand.class) {

            @Override
            public String getSignature(ParameterChangeCommand cmd) {
                String result;
                if (CoordinationConfiguration.doCommandCompletionOnEvent()) {
                    result = getParamChangeSignature(cmd.getPipeline(), cmd.getPipelineElement(), cmd.getParameter(), 
                        cmd.getValue());
                } else {
                    result = null;
                }
                return result;
            }
            
        });
    }

    /**
     * Returns a replay change signature. [helper]
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @param ticket the ticket number
     * @param startReplay starting or stopping the replay
     * @return the signature
     */
    private static String getReplayChangeSignature(String pipeline, String pipelineElement, int ticket, 
        boolean startReplay) {
        return "replay:" + pipeline + ":" + pipelineElement + ":" + ticket + ":" + startReplay;
    }
    
    /**
     * Returns a replay change signature. [helper]
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @param requestedShedder the requested shedder
     * @return the signature
     */
    private static String getLoadSheddingChangeSignature(String pipeline, String pipelineElement, 
        String requestedShedder) {
        return "shed:" + pipeline + ":" + pipelineElement + ":" + requestedShedder;
    }
    
    /**
     * Returns an algorithm change signature. [helper]
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @param algorithm the algorithm
     * @return the signature
     */
    private static String getAlgChangeSignature(String pipeline, String pipelineElement, String algorithm) {
        return "alg:" + pipeline + ":" + pipelineElement + ":" + algorithm;
    }

    /**
     * Returns a parameter change signature. [helper]
     * 
     * @param pipeline the pipeline
     * @param pipelineElement the pipeline element
     * @param parameter the algorithm
     * @param value the value
     * @return the signature
     */
    private static String getParamChangeSignature(String pipeline, String pipelineElement, String parameter, 
        Serializable value) {
        return "param:" + pipeline + ":" + pipelineElement + ":" + parameter; // +value?
    }

    /**
     * Registers the given signature <code>provider</code>.
     * 
     * @param <T> the type of the object handled by the provider
     * @param provider the provider to be registered
     */
    public static <T> void registerProvider(SignatureProvider<T> provider) {
        if (null != provider) {
            PROVIDERS.put(provider.handles(), provider);
        }
    }

    /**
     * Unregisters the given signature <code>provider</code>.
     * 
     * @param <T> the type of the object handled by the provider
     * @param provider the provider
     */
    public static <T> void unregisterProvider(SignatureProvider<T> provider) {
        if (null != provider) {
            PROVIDERS.remove(provider.handles());
        }
    }
    
    /**
     * Unregisters the signature provider for <code>cls</code>.
     * 
     * @param <T> the type of the object handled by the provider
     * @param cls the class the provider handles
     */
    public static <T> void unregisterProvider(Class<T> cls) {
        if (null != cls) {
            PROVIDERS.remove(cls);
        }
    }

    /**
     * Returns the signature for <code>object</code>.
     * 
     * @param obj the object to return the signature for
     * @return the signature or <b>null</b> if there is none
     */
    private static String getSignatureImpl(Object obj) {
        String result = null;
        if (null != obj) {
            SignatureProvider<?> prov = PROVIDERS.get(obj.getClass());
            if (null != prov) {
                result = prov.doSignature(obj);
            }
        }
        return result;
    }

    /**
     * Returns the signature for <code>event</code>.
     * 
     * @param event the event to return the signature for
     * @return the signature or <b>null</b> if there is none
     */
    public static String getSignature(IEnactmentCompletedMonitoringEvent event) {
        return getSignatureImpl(event);
    }
    
    /**
     * Returns the signature for <code>command</code>.
     * 
     * @param command the command to return the signature for
     * @return the signature or <b>null</b> if there is none
     */
    public static String getSignature(CoordinationCommand command) {
        return getSignatureImpl(command);
    }
    
}
