package eu.qualimaster.monitoring;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.MonitoringInformationEvent;
import eu.qualimaster.monitoring.systemState.PlatformSystemPart;
import eu.qualimaster.monitoring.systemState.SystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Realizes an abstract monitoring task as a regular timer task.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractMonitoringTask extends TimerTask {

    private SystemState state;
    private List<IPiggybackTask> tasks = new ArrayList<IPiggybackTask>();
    private boolean disabled = false;

    /**
     * An add-on monitoring task to be executed with the same monitoring task task.
     * 
     * @author Holger Eichelberger
     */
    public interface IPiggybackTask {
        
        /**
         * Collects the monitoring information. This is called by {@link #run()} and secured against
         * exceptions thrown by programming problems.
         */
        public void run();
        
        /**
         * Stops the task.
         */
        public void stop();
        
    }
    
    /**
     * Creates an monitoring task.
     * 
     * @param state the system state to work on
     */
    protected AbstractMonitoringTask(SystemState state) {
        this.state = state;
    }
    
    /**
     * Returns the system state.
     * 
     * @return the systems state
     */
    protected SystemState getState() {
        return state;
    }
    
    /**
     * Returns the execution frequency of the monitoring task.
     * 
     * @return the execution frequency (see {@link MonitoringManager#MINIMUM_MONITORING_FREQUENCY})
     */
    public abstract int getFrequency();
    
    @Override
    public final long scheduledExecutionTime() {
        // disallow modifications
        return super.scheduledExecutionTime();
    }

    /**
     * Sends a demo event to the Adaptation Layer - inform the configuration tool.
     * 
     * @param part the system part to send the data for
     * @param prefix a prefix to be used before the name, e.g., the pipeline name - to be separated by ":"
     * @param required the required state as a combination of the constants defined in the Monitoring Manager
     */
    public static void sendSummaryEvent(SystemPart part, String prefix, int required) {
        if ((MonitoringManager.getDemoMessagesState() & required) != 0) {
            Map<String, Double> data = part.copyObservables(true);
            if (!data.isEmpty()) {
                String name = part.getName();
                if (null != prefix && prefix.length() > 0) {
                    name = prefix + ":" + part.getName();
                }
                if (part instanceof PlatformSystemPart) {
                    PlatformSystemPart platform = (PlatformSystemPart) part;
                    addMachines("machine:", platform.machines(), data);
                    addMachines("hwNode:", platform.hwNodes(), data);
                }
                MonitoringInformationEvent evt = new MonitoringInformationEvent(part.getType().name(), name, data);
                EventManager.send(evt); // assuming that Event Layer was properly started
            }
        }
    }
    
    /**
     * Puts machines to <code>data</code>.
     * 
     * @param prefix prefix to identify the machine kind
     * @param parts the parts to be added to <code>data</code>
     * @param data the collected data (may be modified as a side effect)
     */
    private static void addMachines(String prefix, Collection<? extends SystemPart> parts, Map<String, Double> data) {
        final Double present = 1.0;
        for (SystemPart part : parts) {
            if (part.getObservedValue(ResourceUsage.AVAILABLE) > 0.5) {
                data.put(prefix + part.getName(), present);
            }
        }
    }

    /**
     * Adds the given piggyback task.
     * 
     * @param task the task to be removed (<b>null</b> and already registered tasks are ignored)
     */
    public void add(IPiggybackTask task) {
        if (null != task && !tasks.contains(task)) {
            tasks.add(task);
        }
    }
    
    /**
     * Returns the number of piggyback tasks for this task.
     * 
     * @return the number of piggyback tasks
     */
    public int getPiggybackTaskCount() {
        return tasks.size();
    }
    
    /**
     * Returns the specified piggyback task.
     * 
     * @param index the 0-based index number of the task
     * @return the task
     * @throws IndexOutOfBoundsException if <code>index &lt; 0 || index &gt;= {@link #getPiggybackTaskCount()}</code>
     */
    public IPiggybackTask getPiggybackTask(int index) {
        return tasks.get(index);
    }

    /**
     * Removes the given piggyback task.
     * 
     * @param task the task to be removed (<b>null</b> is ignored)
     */
    public void remove(IPiggybackTask task) {
        if (null != task) {
            tasks.remove(task);
        }
    }
    
    // checkstyle: stop exception type check
    
    @Override
    public final void run() {
        if (!disabled) {
            try {
                monitor();
            } catch (Throwable t) {
                failover(t);
                getLogger().error("During execution of monitoring task " + getClass().getName() , t);
            }
            for (int t = 0; t < tasks.size(); t++) {
                try {
                    tasks.get(t).run();
                } catch (Throwable t1) {
                    getLogger().error("During execution of piggyback task " + tasks.get(t).getClass().getName() , t1);
                }
            }
        }
    }

    // checkstyle: resume exception type check
    
    /**
     * Allows tasks to perform a failover, e.g., when a network connection closed due to failures.
     * 
     * @param th the throwable causing the need for failover
     */
    protected abstract void failover(Throwable th);
    
    /**
     * The main method of this task. This is called by {@link #run()} and secured against
     * exceptions thrown by programming problems.
     */
    protected abstract void monitor(); 
    
    @Override
    public boolean cancel() {
        for (int t = 0; t < tasks.size(); t++) {
            tasks.get(t).stop();
        }
        return super.cancel();
    }

    /**
     * Returns the logger to be used for this class.
     * 
     * @return the logger
     */
    protected Logger getLogger() {
        return LogManager.getLogger(getClass());
    }
    
    /**
     * Reschedules this task to a new frequency/period.
     * 
     * @param frequency the frequency
     */
    protected void reschedule(int frequency) {
        // don't cancel task, just postpone and bring it back if needed
        int freq = Math.max(0, frequency);
        if ((disabled && freq > 0) || !disabled) {
            try {
                Field f = getClass().getField("period");
                f.setAccessible(true);
                f.setLong(this, 0 == freq ? Long.MAX_VALUE : freq);
                disabled = freq == 0;
            } catch (NoSuchFieldException e) {
                // shall not occur
            } catch (IllegalAccessException e) {
                // shall not occur
            }
        }
    }

}
