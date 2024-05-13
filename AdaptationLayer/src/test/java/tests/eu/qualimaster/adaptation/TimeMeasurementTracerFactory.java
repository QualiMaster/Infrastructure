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
package tests.eu.qualimaster.adaptation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.adaptation.internal.AdaptationLoggerFactory;
import net.ssehub.easy.instantiation.core.model.buildlangModel.ITracer;
import net.ssehub.easy.instantiation.core.model.execution.IInstantiatorTracer;
import net.ssehub.easy.instantiation.core.model.execution.TracerFactory;
import net.ssehub.easy.instantiation.rt.core.model.rtVil.DelegatingTracer;

/**
 * A tracer factory which performs time measurements.
 * 
 * @author Holger Eichelberger
 */
public class TimeMeasurementTracerFactory extends TracerFactory {

    /**
     * The potential measures to account for.
     * 
     * @author Holger Eichelberger
     */
    public enum Measure {
        INITIALIZE,
        ANALYSIS,
        BIND,
        STRATEGIES,
        ENACT,
        ADAPT
    }

    /**
     * A measured observation.
     * 
     * @author Holger Eichelberger
     */
    private static class Observation {

        private long start = -1;
        private long time = -1;

        /**
         * Starts/stops measuring.
         * 
         * @param start start measuring or stop
         */
        private void measure(boolean start) {
            if (this.start < 0 && start) {
                this.start = System.currentTimeMillis();
            } else if (this.time < 0 && this.start > 0 && !start) {
                time += System.currentTimeMillis() - this.start;
                this.start = 0;
            }
        }
        
        /**
         * Gets the take time difference as measure.
         * 
         * @return the measure or negative if no measure was completed
         */
        public long getMeasure() {
            return time;
        }
        
        /**
         * Returns whether this observation is actually measuring.
         * 
         * @return <code>true</code> for measuring, <code>false</code> else
         */
        private boolean isMeasuring() {
            return start > 0;
        }
        
    }
    
    /**
     * Represents a set of measurements.
     * 
     * @author Holger Eichelberger
     */
    private static class TimeMeasurements {
        
        private Map<Measure, Observation> observations = new HashMap<Measure, Observation>();

        /**
         * Creates the measurements instance.
         */
        private TimeMeasurements() {
        }
        
        /**
         * Returns whether <code>measure</code> is actually measuring.
         * 
         * @param measure the measure to check
         * @return <code>true</code> for measuring, <code>false</code> else
         */
        private boolean isMeasuring(Measure measure) {
            boolean result = false;
            Observation obs = observations.get(measure);
            if (null != obs) {
                result = obs.isMeasuring();
            }
            return result;
        }
        
        /**
         * Measures time for a given <code>measure</code>.
         * 
         * @param start start or stop the measurement
         * @param measure the measure to account for
         */
        private void measure(boolean start, Measure measure) {
            if (null != measure) {
                Observation obs = observations.get(measure);
                if (null == obs) {
                    obs = new Observation();
                    observations.put(measure, obs);
                }
                obs.measure(start);
            }
        }
        
        /**
         * Returns all observed values.
         * 
         * @return the observations
         */
        private Map<Measure, Long> getObservations() {
            Map<Measure, Long> result = new HashMap<Measure, Long>();
            for (Map.Entry<Measure, Observation> ent : observations.entrySet()) {
                long measure = ent.getValue().getMeasure();
                if (Measure.STRATEGIES == ent.getKey()) {
                    Observation enact = observations.get(Measure.ENACT);
                    if (null != enact) { // overlapping in rt-VIL
                        measure -= enact.getMeasure();
                    }
                }
                result.put(ent.getKey(), measure);
            }
            return result;
        }
    }

    /**
     * A time accounting delegating rt-VIL tracer.
     * 
     * @author Holger Eichelberger
     */
    protected class TimeDelegatingTracer extends DelegatingTracer {

        /**
         * Creates the tracer.
         * 
         * @param tracer the tracer to delegate to
         */
        protected TimeDelegatingTracer(ITracer tracer) {
            super(tracer);
        }
        
        @Override
        public void startBind() {
            measure(true, Measure.BIND, Measure.ANALYSIS);
        }

        @Override
        public void endBind() {
            measure(false, Measure.BIND, Measure.ANALYSIS);
        }

        @Override
        public void startInitialize() {
            measure(true, Measure.INITIALIZE);
        }

        @Override
        public void endInitialize() {
            measure(false, Measure.INITIALIZE);
        }

        @Override
        public void startEnact() {
            measure(true, Measure.ENACT);
        }

        @Override
        public void endEnact() {
            measure(false, Measure.ENACT);
        }

        @Override
        public void startStrategies() {
            measure(true, Measure.STRATEGIES);
        }

        @Override
        public void endStrategies() {
            measure(false, Measure.STRATEGIES);
        }
        
    }

    private static String currentIdentifier;
    private static Map<String, TimeMeasurements> times = new HashMap<String, TimeMeasurements>();
    private TracerFactory factory;

    /**
     * Default constructor for on-demand class loading with no tracing.
     */
    public TimeMeasurementTracerFactory() {
        this(TracerFactory.DEFAULT);
    }
    
    /**
     * Creates a delegating time measurement tracer factory.
     * 
     * @param factory the factory to delegate the tracing to
     */
    public TimeMeasurementTracerFactory(TracerFactory factory) {
        this.factory = factory;
    }

    /**
     * Returns the underlying factory.
     * 
     * @return the factory
     */
    protected TracerFactory getFactory() {
        return factory;
    }

    @Override
    public ITracer createBuildLanguageTracerImpl() {
        return AdaptationLoggerFactory.createTracer(new TimeDelegatingTracer(factory.createBuildLanguageTracerImpl()));
    }

    @Override
    public IInstantiatorTracer createInstantiatorTracerImpl() {
        return factory.createInstantiatorTracerImpl();
    }

    @Override
    public net.ssehub.easy.instantiation.core.model.templateModel.ITracer createTemplateLanguageTracerImpl() {
        return factory.createTemplateLanguageTracerImpl();
    }

    /**
     * Starts/stops measuring for the static current identifier.
     * 
     * @param start start / stop time measurement
     * @param measure the measure to account for
     * @param exclude measures if active excluding <code>measure</code>
     */
    public static void measure(boolean start, Measure measure, Measure...exclude) {
        measure(currentIdentifier, start, measure, exclude);
    }
    
    /**
     * Starts/stops measuring for a given identifier.
     * 
     * @param identifier the identifier
     * @param start start / stop time measurement
     * @param measure the measure to account for
     * @param exclude measures if active excluding <code>measure</code>
     */
    public static void measure(String identifier, boolean start, Measure measure, Measure... exclude) {
        TimeMeasurements measurements = getTimeMeasurements(identifier);
        if (null != measurements) {
            boolean ex = false;
            for (int m = 0; !ex && m < exclude.length; m++) {
                ex = measurements.isMeasuring(exclude[m]);
            }
            if (!ex) {
                measurements.measure(start, measure);
            }
        }
    }

    /**
     * Defines the current identifier to use from tracing.
     * 
     * @param identifier the identifier
     */
    public static void setCurrentIdentifier(String identifier) {
        currentIdentifier = identifier;
    }
    
    /**
     * Returns the time measurements for a given test specification.
     * 
     * @param identifier the measurement identifier
     * @return the time measurements (may be <b>null</b> if no measurements shall be taken)
     */
    public static TimeMeasurements getTimeMeasurements(String identifier) {
        TimeMeasurements result = null;
        if (null != identifier && identifier.length() > 0) {
            result = times.get(identifier);
            if (null == result) {
                result = new TimeMeasurements();
                times.put(identifier, result);
            }
        }
        return result;
    }
    
    /**
     * Returns all identifiers.
     * 
     * @return the identifiers
     */
    public static Collection<String> getIdentifiers() {
        return times.keySet();
    }
 
    /**
     * Returns all observations for a given identifier.
     * 
     * @param identifier the identifier
     * @return the observations
     * @see #getIdentifiers()
     */
    public static Map<Measure, Long> getObservations(String identifier) {
        Map<Measure, Long> result = null;
        if (null != identifier && identifier.length() > 0) {
            TimeMeasurements measures = times.get(identifier);
            if (null != measures) {
                result = measures.getObservations();
            }
        }
        return result;
    }
    
}