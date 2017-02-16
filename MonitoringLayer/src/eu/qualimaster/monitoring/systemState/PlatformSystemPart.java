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
package eu.qualimaster.monitoring.systemState;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.monitoring.events.FrozenSystemState;
import eu.qualimaster.monitoring.parts.PartType;
import eu.qualimaster.monitoring.tracing.ITrace;
import eu.qualimaster.monitoring.tracing.Tracing;
import eu.qualimaster.observables.IObservable;

/**
 * Represents the platform itself.
 * 
 * @author Holger Eichelberger
 */
public class PlatformSystemPart extends SystemPart {
    
    private static final long serialVersionUID = 6873152528009826834L;
    private static transient ITrace trace = Tracing.createInfrastructureTrace();
    private Map<String, MachineSystemPart> machines = new HashMap<String, MachineSystemPart>();
    private Map<String, HwNodeSystemPart> hwClusters = new HashMap<String, HwNodeSystemPart>();
    private Map<String, CloudEnvironmentSystemPart> clouds = new HashMap<String, CloudEnvironmentSystemPart>();
    
    /**
     * Creates a platform system part.
     */
    PlatformSystemPart() {
        super(PartType.PLATFORM, "Infrastructure");
    }

    /**
     * Creates a copy of this system part.
     * 
     * @param source the source system part
     * @param state the parent system state for crosslinks
     */
    protected PlatformSystemPart(PlatformSystemPart source, SystemState state) {
        super(source, state);
        synchronized (hwClusters) {
            for (Map.Entry<String, HwNodeSystemPart> entry : source.hwClusters.entrySet()) {
                this.hwClusters.put(entry.getKey(), new HwNodeSystemPart(entry.getValue(), state));
            }
        }
        synchronized (machines) {
            for (Map.Entry<String, MachineSystemPart> entry : source.machines.entrySet()) {
                this.machines.put(entry.getKey(), new MachineSystemPart(entry.getValue(), state));
            }
        }
        synchronized (clouds) {
            for (Map.Entry<String, CloudEnvironmentSystemPart> entry : source.clouds.entrySet()) {
                this.clouds.put(entry.getKey(), new CloudEnvironmentSystemPart(entry.getValue(), state));
            }
        }
    }
    
    /**
     * Returns the optional trace for the full infrastructure.
     * 
     * @return the trace (may be <b>null</b>)
     */
    public ITrace getTrace() { // not static, akin to algorithms
        return trace;
    }

    /**
     * Resets the trace by closing and recreating it if possible.
     */
    public static void resetTrace() {
        closeTrace();
        trace = Tracing.createInfrastructureTrace();
    }
    
    /**
     * Closes the trace.
     */
    public static void closeTrace() {
        if (null != trace) {
            trace.close();
            trace = null;
        }
    }
    
    /**
     * Returns a general purpose machine system part or creates it if it does not exit.
     * 
     * @param name the name of the machine
     * @return the system part
     */
    public MachineSystemPart obtainMachine(String name) {
        synchronized (machines) { 
            MachineSystemPart result = machines.get(name);
            if (null == result) {
                result = new MachineSystemPart(name);
                machines.put(name, result);
            }
            return result;
        }
    }

    /**
     * Returns a general purpose machine system part.
     * 
     * @param name the name of the machine
     * @return the machine part (may be <b>null</b> if not found)
     */
    public MachineSystemPart getMachine(String name) {
        synchronized (machines) { 
            return machines.get(name);
        }
    }

    /**
     * Removes disappearing machines.
     * 
     * @param keys the names of the disappearing machines
     */
    public void removeMachines(Collection<Object> keys) {
        synchronized (machines) {
            for (Object key : keys) {
                machines.remove(key);
            }
        }
    }
    
    /**
     * Returns all machines.
     * 
     * @return all machines
     */
    public Collection<MachineSystemPart> machines() {
        return machines.values();
    }

    /**
     * Returns a hardware machine system part.
     * 
     * @param name the name of the machine
     * @return the hardware node part (may be <b>null</b> if not found)
     */
    public HwNodeSystemPart getHwNode(String name) {
        synchronized (hwClusters) {
            return hwClusters.get(name);
        }
    }
    
    /**
     * Returns a hardware machine system part or creates it if it does not exist.
     * 
     * @param name the name of the machine
     * @return the system part
     */
    public HwNodeSystemPart obtainHwNode(String name) {
        synchronized (hwClusters) {
            HwNodeSystemPart result = hwClusters.get(name);
            if (null == result) {
                result = new HwNodeSystemPart(name);
                hwClusters.put(name, result);
            }
            return result;
        }
    }

    /**
     * Removes disappearing machines.
     * 
     * @param keys the names of the disappearing nodes
     */
    public void removeHwNode(Collection<Object> keys) {
        synchronized (hwClusters) {
            for (Object key : keys) {
                hwClusters.remove(key);
            }
        }
    }
    
    /**
     * Returns all machines.
     * 
     * @return all machines
     */
    public Collection<HwNodeSystemPart> hwNodes() {
        return hwClusters.values();
    }
    
    /**
     * Returns a cloud environment.
     * 
     * @param name the name of the cloud environment
     * @return the cloud part (may be <b>null</b> if not found)
     */
    public CloudEnvironmentSystemPart getCloudEnvironment(String name) {
        synchronized (clouds) {
            return clouds.get(name);
        }
    }
    
    /**
     * Returns a cloud environment or creates it if it does not exist.
     * 
     * @param name the name of the cloud environment
     * @return the system part
     */
    public CloudEnvironmentSystemPart obtainCloudEnvironment(String name) {
        synchronized (clouds) {
            CloudEnvironmentSystemPart result = clouds.get(name);
            if (null == result) {
                result = new CloudEnvironmentSystemPart(name);
                clouds.put(name, result);
            }
            return result;
        }
    }

    /**
     * Removes disappearing cloud environments.
     * 
     * @param keys the names of the cloud environments
     */
    public void removeCloudEnvironment(Collection<Object> keys) {
        synchronized (clouds) {
            for (Object key : keys) {
                clouds.remove(key);
            }
        }
    }
    
    /**
     * Returns all machines.
     * 
     * @return all machines
     */
    public Collection<CloudEnvironmentSystemPart> cloudEnvironments() {
        return clouds.values();
    }
    
    /**
     * Clears this system part.
     */
    protected void clear() {
        super.clear();
        synchronized (machines) {
            machines.clear();
        }
        synchronized (hwClusters) {
            hwClusters.clear();
        }
        synchronized (clouds) {
            clouds.clear();
        }
    }

    @Override
    protected void fill(String prefix, String name, FrozenSystemState state, Map<IObservable, IOverloadModifier> mods) {
        super.fill(prefix, name, state, mods);
        synchronized (machines) {
            for (Map.Entry<String, MachineSystemPart> entry : machines.entrySet()) {
                entry.getValue().fill(FrozenSystemState.MACHINE, entry.getKey(), state, mods);
            }
        }
        synchronized (hwClusters) {
            for (Map.Entry<String, HwNodeSystemPart> entry : hwClusters.entrySet()) {
                entry.getValue().fill(FrozenSystemState.HWNODE, entry.getKey(), state, mods);
            }
        }
        synchronized (clouds) {
            for (Map.Entry<String, CloudEnvironmentSystemPart> entry : clouds.entrySet()) {
                entry.getValue().fill(FrozenSystemState.CLOUDENV, entry.getKey(), state, mods);
            }
        }
    }

    @Override
    public String toString() {
        String result = super.toString();
        synchronized (machines) {
            result += " machines: " + machines;
        }
        synchronized (hwClusters) {
            result += " clusters: " + hwClusters;
        }
        synchronized (clouds) {
            result += " clouds : " + clouds;
        }
        return result;
    }

    @Override
    public String format(String indent) {
        synchronized (machines) {
            synchronized (hwClusters) {
                return super.format(indent) + "\n machines: " + format(machines, indent + " ") + "\n clusters:" 
                    + format(hwClusters, indent + " ");
            }
        }
    }


}