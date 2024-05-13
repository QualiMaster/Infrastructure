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
package eu.qualimaster.adaptation;

import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.adaptation.events.AlgorithmConfigurationAdaptationEvent;
import eu.qualimaster.adaptation.events.CheckBeforeStartupAdaptationEvent;
import eu.qualimaster.adaptation.events.ParameterConfigurationAdaptationEvent;
import eu.qualimaster.adaptation.events.ReplayAdaptationEvent;
import eu.qualimaster.adaptation.events.SourceVolumeAdaptationEvent;
import eu.qualimaster.adaptation.events.StartupAdaptationEvent;
import eu.qualimaster.adaptation.external.InformationMessage;
import eu.qualimaster.events.IEvent;
import eu.qualimaster.monitoring.events.ConstraintViolationAdaptationEvent;
import eu.qualimaster.monitoring.events.ViolatingClause;

/**
 * Turns adaptation events into information messages.
 * 
 * @author Holger Eichelberger
 */
public class AdaptationEventInformationMessageConverter {
    
    /**
     * A message converter.
     * 
     * @param <E> the event type
     * @author Holger Eichelberger
     */
    public abstract static class MessageConverter <E extends IEvent> {
        
        private Class<E> eventClass;
        
        /**
         * Creates a message converter.
         * 
         * @param cls event class
         */
        protected MessageConverter(Class<E> cls) {
            this.eventClass = cls;
        }

        /**
         * Returns the event class.
         * 
         * @return the event class
         */
        public Class<E> eventClass() {
            return eventClass;
        }

        /**
         * Converts the event.
         * 
         * @param event the event
         * @return the message, may be <b>null</b> for no conversion
         */
        InformationMessage toMessage(IEvent event) {
            InformationMessage result = null;
            if (eventClass.isInstance(event)) {
                result = convert(eventClass.cast(event));
            }
            return result;
        }
        
        /**
         * Converts the event.
         * 
         * @param event the event
         * @return the message, may be <b>null</b> for no conversion
         */
        protected abstract InformationMessage convert(E event);
        
    }
    
    private static final MessageConverter<SourceVolumeAdaptationEvent> SOURCE_VOLUME_CONVERTER 
        = new MessageConverter<SourceVolumeAdaptationEvent>(SourceVolumeAdaptationEvent.class) {

            @Override
            protected InformationMessage convert(SourceVolumeAdaptationEvent event) {
                return new InformationMessage(event.getPipeline(), event.getSource(), 
                     "source volume, normalized findings: " + event.getNormalizedFindings() + " findings: " 
                     + event.getFindings() + " volumes: " + event.getVolumes() + " predictions: " 
                     + event.getPredictions() + " thresholds: " + event.getThresholds());
            }
            
        };

    private static final MessageConverter<CheckBeforeStartupAdaptationEvent> CHECK_STARTUP_CONVERTER 
        = new MessageConverter<CheckBeforeStartupAdaptationEvent>(CheckBeforeStartupAdaptationEvent.class) {

            @Override
            protected InformationMessage convert(CheckBeforeStartupAdaptationEvent event) {
                return new InformationMessage(event.getPipeline(), "", 
                     "startup check adaptation request");
            }
            
        };
        
    private static final MessageConverter<StartupAdaptationEvent> STARTUP_CONVERTER 
        = new MessageConverter<StartupAdaptationEvent>(StartupAdaptationEvent.class) {

            @Override
            protected InformationMessage convert(StartupAdaptationEvent event) {
                return new InformationMessage(event.getPipeline(), "", 
                     "startup adaptation request");
            }
            
        };

    private static final MessageConverter<ReplayAdaptationEvent> REPLAY_CONVERTER 
        = new MessageConverter<ReplayAdaptationEvent>(ReplayAdaptationEvent.class) {

            @Override
            protected InformationMessage convert(ReplayAdaptationEvent event) {
                return new InformationMessage(event.getPipeline(), event.getPipelineElement(), 
                     "replay adaptation request: " + event.getTicket() + " " + event.getSpeed() 
                     + " " + event.getQuery());
            }
            
        };
        
    private static final MessageConverter<AlgorithmConfigurationAdaptationEvent> ALGORITHM_CONFIGURATION_CONVERTER 
        = new MessageConverter<AlgorithmConfigurationAdaptationEvent>(AlgorithmConfigurationAdaptationEvent.class) {

            @Override
            protected InformationMessage convert(AlgorithmConfigurationAdaptationEvent event) {
                return new InformationMessage(event.getPipeline(), event.getPipelineElement(), 
                     "requested algorithm: " + event.getAlgorithm());
            }
            
        };

    private static final MessageConverter<ParameterConfigurationAdaptationEvent> PARAMETER_CONFIGURATION_CONVERTER 
        = new MessageConverter<ParameterConfigurationAdaptationEvent>(ParameterConfigurationAdaptationEvent.class) {

            @Override
            protected InformationMessage convert(ParameterConfigurationAdaptationEvent event) {
                return new InformationMessage(event.getPipeline(), event.getPipelineElement(), 
                     "requested parameter change: " + event.getParameter() + " " + event.getValue());
            }
            
        };
        
    private static final MessageConverter<ConstraintViolationAdaptationEvent> CONSTRAINT_VIOLATION_CONVERTER 
        = new MessageConverter<ConstraintViolationAdaptationEvent>(ConstraintViolationAdaptationEvent.class) {

            @Override
            protected InformationMessage convert(ConstraintViolationAdaptationEvent event) {
                String pipelines = "";
                String variables = "";
                String description = "";
                for (int v = 0; v < event.getViolatingClauseCount(); v++) {
                    ViolatingClause c = event.getViolatingClause(v);
                    if (description.length() > 0) {
                        description += ", ";
                        pipelines += ", ";
                        variables += ", ";
                    }
                    pipelines += c.getPipeline();
                    pipelines += c.getVariable();
                    description += c.getObservable() + " " + c.getDeviation();
                }
                return new InformationMessage(pipelines, variables, description);
            }
        
        };
    
    private static final Map<Class<?>, MessageConverter<?>> CONVERTERS = new HashMap<Class<?>, MessageConverter<?>>();

    /**
     * Turns an event into a message.
     * 
     * @param event the event
     * @return the message (may be <b>null</b> for none)
     */
    public static InformationMessage toMessage(IEvent event) {
        InformationMessage msg = null;
        if (null != event) {
            MessageConverter<?> conv = CONVERTERS.get(event.getClass());
            if (null != conv) {
                msg = conv.toMessage(event);
            }
        }
        if (null == msg) {
            msg = new InformationMessage("", "", event.toString());
        }
        return msg;
    }
    
    /**
     * Registers a converter.
     * 
     * @param converter the converter instance
     */
    public static void registerConverter(MessageConverter<?> converter) {
        if (null != converter && null != converter.getClass()) {
            CONVERTERS.put(converter.getClass(), converter);
        }
    }
    
    static {
        registerConverter(SOURCE_VOLUME_CONVERTER);
        registerConverter(CONSTRAINT_VIOLATION_CONVERTER);
        registerConverter(CHECK_STARTUP_CONVERTER);
        registerConverter(STARTUP_CONVERTER);
        registerConverter(REPLAY_CONVERTER);
        registerConverter(ALGORITHM_CONFIGURATION_CONVERTER);
        registerConverter(PARAMETER_CONFIGURATION_CONVERTER);
    }
    
}
