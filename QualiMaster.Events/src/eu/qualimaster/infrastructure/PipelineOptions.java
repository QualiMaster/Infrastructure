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
package eu.qualimaster.infrastructure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.common.QMSupport;

/**
 * Stores pipeline startup / shutdown options. Please note that the names used in this
 * class are executor names, i.e., no name mapping happens and the element names must actually
 * be implementation names. Please note that currently executor arguments may only considered
 * in the generated pipelines if they have a default value.
 * 
 * @author Holger Eichelberger
 */
@QMSupport
public class PipelineOptions implements Serializable {
    
    public static final String SEPARATOR = ".";
    public static final String KEY_WORKERS = "numWorkers";
    public static final String KEY_MAINPIP = "mainPipeline";
    public static final String KEY_PROFILINGMODE = "profilingMode";
    public static final String PREFIX_EXECUTOR = "executor" + SEPARATOR;
    public static final String KEY_WAIT_TIME = "waitTime";
    public static final String SUFFIX_PARLLELISM = SEPARATOR + "paralellism";
    public static final String SUFFIX_TASKS = SEPARATOR + "tasks";
    public static final String SUFFIX_ARGUMENT = SEPARATOR + "arg";
    public static final String KEY_ADAPTATION = "qm.adaptation";

    private static final long serialVersionUID = 7622146883311355571L;
    private Map<String, Serializable> options = new HashMap<String, Serializable>();
    
    /**
     * Creates a pipeline option object (adaptation enabled).
     */
    public PipelineOptions() {
        this((Class<? extends AdaptationEvent>) null);
    }

    /**
     * Creates a pipeline option object.
     * 
     * @param adaptationFilter the event class acting as instanceof-filter letting only selected adaptation events
     * through, can be <b>null</b> if no filter applies, can be {@link AdaptationEvent} to disable adaptation for this 
     * pipeline
     */
    public PipelineOptions(Class<? extends AdaptationEvent> adaptationFilter) {
        setAdaptationFilter(adaptationFilter);
    }

    /**
     * Parses a pipeline option object from command line.
     * 
     * @param args the arguments to be parsed
     */
    public PipelineOptions(String[] args) {
        this();
        int a = 0;
        while (a + 1 < args.length) { // name + param
            boolean successful = true;
            String arg = args[a];
            a++; // increase anyway, endless loop else
            try {
                if (arg.equals(KEY_WORKERS)) {
                    setNumberOfWorkers(parseIntArg(args, a));
                } else if (arg.equals(KEY_PROFILINGMODE)) {
                    if (parseBooleanArg(args, a)) {
                        enableProfilingMode();
                    }
                } else if (arg.equals(KEY_WAIT_TIME)) {
                    setWaitTime(parseIntArg(args, a));
                } else if (arg.startsWith(PREFIX_EXECUTOR)) {
                    if (arg.endsWith(SUFFIX_PARLLELISM)) {
                        setExecutorParallelism(extractExecutor(arg, PREFIX_EXECUTOR, SUFFIX_PARLLELISM), 
                            parseIntArg(args, a));
                    } else if (arg.endsWith(SUFFIX_TASKS)) {
                        setTaskParallelism(extractExecutor(arg, PREFIX_EXECUTOR, SUFFIX_TASKS), 
                            parseIntArg(args, a));
                    } else if (arg.endsWith(SUFFIX_ARGUMENT)) {
                        setExecutorArgument(extractExecutor(arg, PREFIX_EXECUTOR, SUFFIX_ARGUMENT), args[a]);
                    } else {
                        successful = false;
                    }
                } else if (arg.equals(KEY_ADAPTATION)) {
                    setAdaptationFilter(parseStringArg(args, a));
                } else {
                    successful = false;
                }
            } catch (NumberFormatException e) {
                successful = false; // ignore, try next
            }
            if (successful) {
                a++; // advance if successful
            }
        }
    }

    /**
     * Creates a pipeline options object from <code>opts</code> by copying the settings (for now, just the mappings 
     * are copied, not the values).
     * 
     * @param opts the pipeline options
     */
    public PipelineOptions(PipelineOptions opts) {
        this.options.putAll(opts.options);
    }
    
    /**
     * Marks the related pipeline as loosely integrated sub-pipeline.
     * 
     * @param mainPipeline the name of the main pipeline
     */
    public void markAsSubPipeline(String mainPipeline) {
        options.put(KEY_MAINPIP, mainPipeline);
    }

    /**
     * Returns whether this pipeline is marked as a loosely integrated sub-pipeline.
     * 
     * @return <code>true</code> for sub-pipeline, <code>false</code> else
     * @see PipelineOptions#markAsSubPipeline(String)
     */
    public boolean isSubPipeline() {
        return isSubPipeline(getMainPipeline());
    }
    
    /**
     * Returns the name of the main pipeline in case of a sub pipeline.
     * 
     * @return the name of the main pipeline, if not a sub-pipeline empty or <b>null</b>
     * @see PipelineOptions#markAsSubPipeline(String)
     */
    public String getMainPipeline() {
        return getStringValue(options.get(KEY_MAINPIP), null);
    }

    /**
     * Defines the adaptation filter.
     * 
     * @param adaptationFilter the event class acting as instanceof-filter letting only selected adaptation events
     * through, can be <b>null</b> if no filter applies, can be {@link AdaptationEvent} to disable adaptation for this 
     * pipeline
     */
    private void setAdaptationFilter(Class<? extends AdaptationEvent> adaptationFilter) {
        setAdaptationFilter(null == adaptationFilter ? null : adaptationFilter.getName());
    }
    
    /**
     * Defines whether adaptation for this pipeline shall be enabled.
     * 
     * @param adaptationFilterClass the name of the event class acting as instanceof-filter letting only selected 
     * adaptation events through, can be <b>null</b> if no filter applies, can be {@link AdaptationEvent} to disable 
     * adaptation for this pipeline
     */
    private void setAdaptationFilter(String adaptationFilterClass) {
        options.put(KEY_ADAPTATION, adaptationFilterClass);
    }
    
    /**
     * Returns the name of the adaptation filter.
     * 
     * @return the name of the adaptation filter (may be <b>null</b>)
     */
    public String getAdaptationFilterName() {
        String result;
        Object tmp = options.get(KEY_ADAPTATION);
        if (null != tmp) {
            result = tmp.toString();
        } else {
            result = null;
        }
        return result;
    }
    
    /**
     * Returns whether adaptation shall be enabled for this pipeline.
     * 
     * @return <code>true</code> if adaptation shall be enabled (default), <code>false</code> if adaptation shall be
     * disabled
     */
    public Class<? extends AdaptationEvent> getAdaptationFilter() {
        return getAdaptationFilter(getAdaptationFilterName());
    }

    /**
     * Returns the adaptation filter for the given filter class name.
     * 
     * @param filterClassName the name of the adaptation filter (may be <b>null</b>)
     * @return the corresponding adaptation filter, <b>null</b> if there is none or <code>filterClassName</code>
     * cannot be turned into an adaptation event class
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends AdaptationEvent> getAdaptationFilter(String filterClassName) {
        Class<? extends AdaptationEvent> result = null;
        if (null != filterClassName) {
            try {
                Class<?> tmpCls = Class.forName(filterClassName);
                if (AdaptationEvent.class.isAssignableFrom(tmpCls)) {
                    result = (Class<? extends AdaptationEvent>) tmpCls;
                } else {
                    Logger.getLogger(tmpCls.getName() + " is not a subclass of " + AdaptationEvent.class.getName());
                }
            } catch (ClassNotFoundException e) {
                Logger.getLogger(PipelineOptions.class).error(e.getMessage(), e);
            }
        } 
        return result;
    }

    /**
     * Parses an int argument value.
     * 
     * @param args the arguments
     * @param keyPos the index position of the argument in <code>args</code>
     * @return the parsed int argument value
     * @throws NumberFormatException in case that parsing fails
     */
    private static int parseIntArg(String[] args, int keyPos) throws NumberFormatException {
        return Integer.parseInt(args[keyPos]);
    }
    
    /**
     * Parses a Boolean argument value.
     * 
     * @param args the arguments
     * @param keyPos the index position of the argument in <code>args</code>
     * @return the parsed Boolean argument value
     */
    private static Boolean parseBooleanArg(String[] args, int keyPos) {
        return Boolean.valueOf(args[keyPos]);
    }
    
    /**
     * Parses a String argument value.
     * 
     * @param args the arguments
     * @param keyPos the index position of the argument in <code>args</code>
     * @return the parsed String argument value (may be <b>null</b>)
     */
    private static String parseStringArg(String[] args, int keyPos) {
        Object tmp = args[keyPos];
        return null == tmp ? null : tmp.toString();
    }

    /**
     * Extracts the executor. Prerequisite is that <code>key</code> starts with <code>prefix</code>.
     * 
     * @param key the argument key
     * @param prefix the prefix
     * @param suffix the suffix
     * @return the executor name, <b>null</b> else
     */
    private static String extractExecutor(String key, String prefix, String suffix) {
        String result;
        String tmp = key.substring(prefix.length());
        int end = tmp.length() - suffix.length();
        if (end > 0 && end < tmp.length()) {
            result = tmp.substring(0, tmp.length() - suffix.length());
        } else {
            result = null;
        }
        return result;
    }
    
    /**
     * Changes to profiling mode. If this method is called once, it is not intended to change it back.
     */
    public void enableProfilingMode() {
        options.put(KEY_PROFILINGMODE, Boolean.TRUE); 
    }
    
    /**
     * Returns whether the pipeline characterized by this option object is in profiling mode.
     * 
     * @return <code>true</code> for profiling mode, <code>false</code> else (default)
     */
    public boolean isInProfilingMode() {
        return getBooleanValue(KEY_PROFILINGMODE, false);
    }
    
    /**
     * Defines the number of workers.
     * 
     * @param numWorkers the number of workers
     */
    public void setNumberOfWorkers(int numWorkers) {
        options.put(KEY_WORKERS, numWorkers);
    }
    
    /**
     * Defines the operation waiting time.
     * 
     * @param time the operation waiting time (s)
     */
    public void setWaitTime(int time) {
        options.put(KEY_WAIT_TIME, Math.max(0, time));
    }
    
    /**
     * Returns the operation waiting time.
     * 
     * @param dflt the default value (s)
     * @return the waiting time (s), <code>dflt</code> if not given 
     */
    public int getWaitTime(int dflt) {
        return getIntValue(KEY_WAIT_TIME, dflt);
    }
    
    /**
     * Returns an Boolean option value.
     * 
     * @param key the key to look for
     * @param dflt the default value in case that no option is given
     * @return the Boolean value or, if not in options, <code>dflt</code>
     */
    private boolean getBooleanValue(String key, boolean dflt) {
        boolean result = dflt;
        Object val = options.get(key);
        if (val instanceof Boolean) {
            result = ((Boolean) val).booleanValue();
        } else if (null != val) {
            String sVal = val.toString().toLowerCase();
            if (Boolean.toString(true).equals(sVal)) {
                result = true;
            } else if (Boolean.toString(false).equals(sVal)) {
                result = false;
            }
        }
        return result;
    }
    
    /**
     * Returns an int option value.
     * 
     * @param key the key to look for
     * @param dflt the default value in case that no option is given
     * @return the int value or, if not in options, <code>dflt</code>
     */
    private int getIntValue(String key, int dflt) {
        int result;
        Number number = getNumberValue(key, dflt);
        if (null == number) {
            result = dflt;
        } else {
            result = number.intValue();
        }
        return result;
    }

    /**
     * Returns an option number value.
     * 
     * @param key the key to look for
     * @param dflt the default value in case that no option is given
     * @return the number value or, if not in options, <code>dflt</code>
     */
    private Number getNumberValue(String key, Number dflt) {
        Number result = dflt;
        Object tmp = options.get(key);
        if (tmp instanceof Integer) {
            result = (Integer) tmp;
        } else if (null != tmp) {
            try {
                result = Integer.parseInt(tmp.toString());
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return result;
    }

    /**
     * Returns the number of pipeline workers.
     * 
     * @param dflt the default value (e.g., from IVML config)
     * @return the numbers of pipelines, if none is in the options return <code>deflt</code>)
     */
    public int getNumberOfWorkers(int dflt) {
        return getIntValue(KEY_WORKERS, dflt);
    }

    /**
     * Defines the executor parallelism for the given <code>executor</code>.
     * 
     * @param executor the executor name (may be <b>null</b>, ignored then)
     * @param parallelism the desired parallelism, i.e., the number of executors
     */
    public void setExecutorParallelism(String executor, int parallelism) {
        if (null != executor) {
            options.put(getExecutorParallelismKey(executor), parallelism);
        }
    }
    
    /**
     * Returns the desired executor parallelism.
     * 
     * @param executor the name of the executor
     * @param dflt the default parallelism (e.g., from IVML config)
     * @return the task executor parallelism value, if none is in the options use <code>deflt</code>)
     */
    public int getExecutorParallelism(String executor, int dflt) {
        return getIntValue(getExecutorParallelismKey(executor), dflt);
    }

    /**
     * Returns the desired executor parallelism.
     * 
     * @param executor the name of the executor
     * @param dflt the default parallelism (e.g., from IVML config)
     * @return the task executor parallelism value, if none is in the options use <code>deflt</code>)
     */
    public Number getExecutorParallelism(String executor, Number dflt) {
        return getNumberValue(getExecutorParallelismKey(executor), dflt);
    }
    
    /**
     * Defines the task parallelism for the given <code>executor</code>.
     * 
     * @param executor the executor name (may be <b>null</b>, ignored then)
     * @param taskParallelism the desired task parallelism
     */
    public void setTaskParallelism(String executor, int taskParallelism) {
        if (null != executor) {
            options.put(getTaskParallelismKey(executor), taskParallelism);
        }
    }
    
    /**
     * Returns the desired task parallelism.
     * 
     * @param executor the name of the executor
     * @param dflt the default value (e.g., from IVML config)
     * @return the task parallelism value, if none is in the options use <code>deflt</code>)
     */
    public int getTaskParallelism(String executor, int dflt) {
        return getIntValue(getTaskParallelismKey(executor), dflt);
    }

    /**
     * Returns the desired task parallelism.
     * 
     * @param executor the name of the executor
     * @param dflt the default value (e.g., from IVML config)
     * @return the task parallelism value, if none is in the options use <code>deflt</code>)
     */
    public Number getTaskParallelism(String executor, Number dflt) {
        return getNumberValue(getTaskParallelismKey(executor), dflt);
    }

    /**
     * Returns the parallelism option key for the given <code>executor</code>.
     * 
     * @param executor the name of the executor
     * @return the option key
     */
    private static String getTaskParallelismKey(String executor) {
        return PREFIX_EXECUTOR + executor + SUFFIX_TASKS;
    }
    
    /**
     * Returns the parallelism option key for the given <code>executor</code>.
     * 
     * @param executor the name of the executor
     * @return the option key
     */
    public static String getExecutorParallelismKey(String executor) {
        return PREFIX_EXECUTOR + executor + SUFFIX_PARLLELISM;
    }
    
    /**
     * Returns the key for an arbitrary executor parameter value.
     * 
     * @param executor the executor name
     * @param paramName the parameter name
     * @return the key
     */
    public static String getExecutorArgumentKey(String executor, String paramName) {
        return PREFIX_EXECUTOR + executor + SEPARATOR + paramName + SUFFIX_ARGUMENT;
    }
    
    /**
     * Returns the combination of executor and param name for the given combined key. This is a counter function to 
     * {@link #getExecutorArgumentKey(String, String)}.
     * 
     * @param executorArgumentKey the key to process
     * @return <b>null</b> if no split is possible (illegal format), the executor and param name else
     */
    public static String getExecutorParamName(String executorArgumentKey) {
        String result = null;
        if (executorArgumentKey.length() > PREFIX_EXECUTOR.length() + SUFFIX_ARGUMENT.length() 
            && executorArgumentKey.startsWith(PREFIX_EXECUTOR) && executorArgumentKey.endsWith(SUFFIX_ARGUMENT)) {
            result = executorArgumentKey.substring(PREFIX_EXECUTOR.length(), 
                executorArgumentKey.length() - SUFFIX_ARGUMENT.length());
        }
        return result;
    }

    /**
     * Defines the value of an arbitrary executor parameter.
     * 
     * @param executor the executor name
     * @param paramName the parameter name
     * @param value the value of <code>paramName</code>
     */
    public void setExecutorArgument(String executor, String paramName, Serializable value) {
        if (null != executor && null != paramName) {
            options.put(getExecutorArgumentKey(executor, paramName), value);
        }
    }

    /**
     * Defines the value of an arbitrary executor parameter.
     * 
     * @param executorParamName executor name and parameter name separated by {@link #SEPARATOR}
     * @param value the value of <code>paramName</code> 
     */
    public void setExecutorArgument(String executorParamName, Serializable value) {
        if (null != executorParamName) {
            int pos = executorParamName.indexOf(SEPARATOR);
            if (pos >= 0 && pos < executorParamName.length()) {
                String executor = executorParamName.substring(0, pos);
                String paramName = executorParamName.substring(pos + 1, executorParamName.length());
                setExecutorArgument(executor, paramName, value);
            }
        }
    }
    
    /**
     * Returns the value of an arbitrary executor parameter.
     * 
     * @param executor the executor name
     * @param paramName the parameter name
     * @return the value, <b>null</b> if no value is known
     */
    public Serializable getExecutorArgument(String executor, String paramName) {
        Serializable result;
        if (null != executor && null != paramName) {
            result = options.get(getExecutorArgumentKey(executor, paramName));
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Returns whether an arbitrary executor parameter was configured.
     * 
     * @param executor the executor name
     * @param paramName the parameter name
     * @return <code>true</code> of the executor parameter was configured, <code>false</code> else
     */
    public boolean hasExecutorArgument(String executor, String paramName) {
        boolean result;
        if (null != executor && null != paramName) {
            result = options.containsKey(getExecutorArgumentKey(executor, paramName));
        } else {
            result = false;
        }
        return result;
    }
    
    /**
     * Turns those options to be passed on through a topology into a Storm conf. In particular, executor 
     * arguments are turned into the <code>conf</code>.
     *  
     * @param conf the conf to be filled
     * @return <code>conf</code> if conf is not <b>null</b>, a new map if there were options to be stored, 
     *     <b>null</b> else
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Map toConf(Map conf) {
        for (Map.Entry<String, Serializable> entry : options.entrySet()) {
            String key = entry.getKey();
            if (isConfKey(key)) {
                if (null == conf) {
                    conf = new HashMap();
                }
                conf.put(key, entry.getValue());
            }
        }
        return conf;
    }

    /**
     * Returns whether <code>key</code> is a key to be turned into a pipeline conf.
     *  
     * @param key the key
     * @return <code>true</code> for pipeline conf, <code>false</code> for arguments
     */
    public static final boolean isConfKey(String key) {
        return key.startsWith(PREFIX_EXECUTOR);
    }

    /**
     * Turns all options into command line arguments using <code>toString</code> for each value.
     *  
     * @param pipelineName the name of the pipeline to be passed as first argument of a distributed
     *   execution by convention, may be <b>null</b> for local execution and is ignored then
     * @return the command line arguments
     */
    public String[] toArgs(String pipelineName) {
        List<String> args = new ArrayList<String>();
        if (null != pipelineName) {
            args.add(pipelineName);
        }
        for (Map.Entry<String, Serializable> entry : options.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (null != val) {
                args.add(key);
                args.add(val.toString());
            }
        }
        return args.toArray(new String[args.size()]);
    }
    
    @Override
    public boolean equals(Object object) {
        boolean result = false;
        if (object instanceof PipelineOptions) {
            PipelineOptions other = (PipelineOptions) object;
            result = options.equals(other.options);
        }
        return result;
    }
    
    @Override
    public int hashCode() {
        return options.hashCode();
    }
    
    /**
     * Turns all (!) options into a map.
     * 
     * @return the related storm configuration map
     */
    public Map<String, Serializable> toMap() {
        Map<String, Serializable> result = new HashMap<String, Serializable>();
        result.putAll(options);
        return result;
    }
    
    @Override
    public String toString() {
        return "PipelineOptions: " + options;
    }

    /**
     * Returns whether there is an executor argument.
     * 
     * @param map the map (Storm conf) containing the name-value binding
     * @param executor the executor name
     * @param paramName the parameter name
     * @return <code>true</code> if there is an executor argument, <code>false</code> else
     */
    public static boolean hasExecutorArgument(@SuppressWarnings("rawtypes") Map map, String executor, 
        String paramName) {
        return map.containsKey(getExecutorArgumentKey(executor, paramName));
    }
    
    /**
     * Returns an executor argument as Double.
     * 
     * @param map the map (Storm conf) containing the name-value binding
     * @param executor the executor name
     * @param paramName the parameter name
     * @param deflt the default value to be returned if the argument was not specified or cannot be converted
     * @return the actual value or <code>deflt</code> if no argument was specified
     */
    public static int getExecutorIntArgument(@SuppressWarnings("rawtypes") Map map, String executor, 
        String paramName, int deflt) {
        return getIntValue(map.get(getExecutorArgumentKey(executor, paramName)), deflt);
    }

    /**
     * Turns the configuration <code>value</code> into a integer value.
     * 
     * @param value the value
     * @param deflt the default value if <code>value</code> is <b>null</b> or cannot be converted
     * @return the converted value or <code>deflt</code>
     */
    private static int getIntValue(Object value, int deflt) {
        int result = deflt;
        if (null != value) {
            if (value instanceof Integer) {
                result = ((Integer) value).intValue();
            } else {
                try {
                    result = Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return result;
    }

    /**
     * Returns an executor argument as Double.
     * 
     * @param map the map (Storm conf) containing the name-value binding
     * @param executor the executor name
     * @param paramName the parameter name
     * @param deflt the default value to be returned if the argument was not specified or cannot be converted
     * @return the actual value or <code>deflt</code> if no argument was specified
     */
    public static double getExecutorDoubleArgument(@SuppressWarnings("rawtypes") Map map, String executor, 
        String paramName, double deflt) {
        return getDoubleValue(map.get(getExecutorArgumentKey(executor, paramName)), deflt);
    }

    /**
     * Turns the configuration <code>value</code> into a double value.
     * 
     * @param value the value
     * @param deflt the default value if <code>value</code> is <b>null</b> or cannot be converted
     * @return the converted value or <code>deflt</code>
     */
    private static double getDoubleValue(Object value, double deflt) {
        double result = deflt;
        if (null != value) {
            if (value instanceof Double) {
                result = ((Double) value).intValue();
            } else {
                try {
                    result = Double.parseDouble(value.toString());
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return result;
    }

    /**
     * Returns an executor argument as Long.
     * 
     * @param map the map (Storm conf) containing the name-value binding
     * @param executor the executor name
     * @param paramName the parameter name
     * @param deflt the default value to be returned if the argument was not specified or cannot be converted
     * @return the actual value or <code>deflt</code> if no argument was specified
     */
    public static long getExecutorLongArgument(@SuppressWarnings("rawtypes") Map map, String executor, 
        String paramName, long deflt) {
        return getLongValue(map.get(getExecutorArgumentKey(executor, paramName)), deflt);
    }

    /**
     * Turns the configuration <code>value</code> into a long value.
     * 
     * @param value the value
     * @param deflt the default value if <code>value</code> is <b>null</b> or cannot be converted
     * @return the converted value or <code>deflt</code>
     */
    private static long getLongValue(Object value, long deflt) {
        long result = deflt;
        if (null != value) {
            if (value instanceof Long) {
                result = ((Long) value).longValue();
            } else {
                try {
                    result = Long.parseLong(value.toString());
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return result;
    }

    /**
     * Returns an executor argument as Boolean value.
     * 
     * @param map the map (Storm conf) containing the name-value binding
     * @param executor the executor name
     * @param paramName the parameter name
     * @param deflt the default value to be returned if the argument was not specified or cannot be converted
     * @return the actual value or <code>deflt</code> if no argument was specified
     */
    public static boolean getExecutorBooleanArgument(@SuppressWarnings("rawtypes") Map map, String executor, 
        String paramName, boolean deflt) {
        return getBooleanValue(map.get(getExecutorArgumentKey(executor, paramName)), deflt);
    }

    /**
     * Turns the configuration <code>value</code> into a Boolean.
     * 
     * @param value the value
     * @param deflt the default value if <code>value</code> is <b>null</b> or cannot be converted
     * @return the converted value or <code>deflt</code>
     */
    private static boolean getBooleanValue(Object value, boolean deflt) {
        boolean result = deflt;
        if (null != value) {
            if (value instanceof Boolean) {
                result = ((Boolean) value).booleanValue();
            } else {
                result = Boolean.parseBoolean(value.toString());
            }
        }
        return result;
    }
    
    /**
     * Returns an executor argument as String.
     * 
     * @param map the map (Storm conf) containing the name-value binding
     * @param executor the executor name
     * @param paramName the parameter name
     * @param deflt the default value to be returned if the argument was not specified or cannot be converted
     * @return the actual value or <code>deflt</code> if no argument was specified
     */
    public static String getExecutorStringArgument(@SuppressWarnings("rawtypes") Map map, String executor, 
        String paramName, String deflt) {
        return getStringValue(map.get(getExecutorArgumentKey(executor, paramName)), deflt);
    }

    /**
     * Turns the configuration <code>value</code> into a string.
     * 
     * @param value the value
     * @param deflt the default value if <code>value</code> is <b>null</b> or cannot be converted
     * @return the converted value or <code>deflt</code>
     */
    private static String getStringValue(Object value, String deflt) {
        String result = deflt;
        if (null != value) {
            result = value.toString();
        }
        return result;
    }

    /**
     * Returns the desired executor parallelism.
     * 
     * @param map the map (Storm conf) containing the name-value binding
     * @param executor the name of the executor
     * @param dflt the default value (e.g., from IVML config)
     * @return the executor parallelism value, if none is in the options use <code>deflt</code>)
     */
    public static int getExecutorParallelism(@SuppressWarnings("rawtypes") Map map, String executor, int dflt) {
        return getIntValue(map.get(getExecutorParallelismKey(executor)), dflt);
    }

    /**
     * Returns the desired task parallelism.
     * 
     * @param map the map (Storm conf) containing the name-value binding
     * @param executor the name of the executor
     * @param dflt the default value (e.g., from IVML config)
     * @return the task parallelism value, if none is in the options use <code>deflt</code>)
     */
    public static int getTaskParallelism(@SuppressWarnings("rawtypes") Map map, String executor, int dflt) {
        return getIntValue(map.get(getTaskParallelismKey(executor)), dflt);
    }

    /**
     * Returns the desired number of workers.
     * 
     * @param map the map (Storm conf) containing the name-value binding
     * @param dflt the default value (e.g., from IVML config)
     * @return the task number of workers, if none is in the options use <code>deflt</code>)
     */
    public static int getNumberOfWorkers(@SuppressWarnings("rawtypes") Map map, int dflt) {
        return getIntValue(map.get(KEY_WORKERS), dflt);
    }
    
    /**
     * In case of a sub-pipeline, the name of the including main pipeline.
     * 
     * @param map the map (Storm conf) containing the name-value binding
     * @return the name of the main pipeline, may be <b>null</b> or empty if this is not a sub-pipeline
     */
    public static String getMainPipeline(@SuppressWarnings("rawtypes") Map map) {
        return getStringValue(map.get(KEY_MAINPIP), null);
    }
    
    /**
     * Returns whether a pipeline based on its <code>mainPipeline</code> is a sub-pipeline.
     * 
     * @param mainPipeline the main-pipeline
     * @return <code>true</code> for sub-pipeline, <code>false</code> else
     */
    public static boolean isSubPipeline(String mainPipeline) {
        return null != mainPipeline && mainPipeline.length() > 0;
    }
    
    /**
     * Merges the given options into this options set. <code>opts</code> take precedence over already specified 
     * options!
     * 
     * @param opts the options to merge into (may be <b>null</b>, then nothing happens)
     */
    public void merge(PipelineOptions opts) {
        if (null != opts) {
            options.putAll(opts.options);
        }
    }
    
}