package eu.qualimaster.monitoring.observations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import eu.qualimaster.monitoring.events.IRemovalSelector;

/**
 * Implements a compound observation, mapping all null keys to a single value. This class reacts on 
 * {@link IRemovalSelector} on the keys.
 * 
 * @author Holger Eichelberger
 */
public abstract class AbstractCompoundObservation implements IObservation {

    public static final Object NULL_KEY = new Serializable() {

        private static final long serialVersionUID = -2674504495287314016L;
    };
    
    private static final long serialVersionUID = -5216597956347539458L;
    private AtomicLong lastUpdate = new AtomicLong(-1);

    private Map<Object, AtomicDouble> components = Collections.synchronizedMap(new HashMap<Object, AtomicDouble>());
    private List<IObservation> links;
    
    /**
     * Creates an abstract compound observation.
     * 
     * @see #clear()
     */
    protected AbstractCompoundObservation() {
        clear();
    }

    /**
     * Creates a compound observation from a given source.
     * 
     * @param source the source to copy from
     */
    protected AbstractCompoundObservation(AbstractCompoundObservation source) {
        Set<Object> keys = new HashSet<Object>();
        keys.addAll(source.components.keySet());
        for (Object key : keys) {
            AtomicDouble sourceVal = source.components.get(key);
            if (null != sourceVal) {
                this.components.put(key, new AtomicDouble(sourceVal.get()));
            }
        }
        this.lastUpdate.set(source.lastUpdate.get());
        if (null != source.links) {
            this.links = new ArrayList<IObservation>(source.links.size());
            this.links.addAll(source.links);
        }
    }
    
    @Override
    public boolean isComposite() {
        return true;
    }

    @Override
    public void setValue(double value, Object key) {
        changeValueImpl(value, key, false);
    }

    @Override
    public void setValue(Double value, Object key) {
        if (null != value) {
            changeValueImpl(value, key, false);
        }
    }

    /**
     * Changes the value according to the compound key.
     * 
     * @param value the new value
     * @param inc set or increment
     * @param key the compound key
     */
    private void changeValueImpl(double value, Object key, boolean inc) {
        boolean done = false;
        if (null != links) {
            for (int l = 0; l < links.size(); l++) {
                IObservation obs = links.get(l);
                if (null != obs.getValue(key)) {
                    if (inc) {
                        obs.incrementValue(value, key);
                    } else {
                        obs.setValue(value, key);
                    }
                    done = true;
                }
            }
        }
        if (!done) {
            key = checkKey(key);
            AtomicDouble val = components.get(key);
            if (null == val) {
                val = new AtomicDouble(value);
                put(key, val);
            } else {
                if (inc) {
                    val.addAndGet(value);
                } else {
                    val.set(value);
                }
            }
        }
        lastUpdate.set(System.currentTimeMillis());
    }

    /**
     * Updates the actual value.
     * 
     * @param key the compound key
     * @param value the new value
     */
    private void put(Object key, AtomicDouble value) {
        if (key instanceof IRemovalSelector && components.containsKey(key)) {
            Iterator<Object> iter = components.keySet().iterator();
            while (iter.hasNext()) {
                Object k = iter.next();
                if (k instanceof IRemovalSelector && ((IRemovalSelector) k).remove(key)) {
                    iter.remove();
                }
            }
        }
        components.put(key, value);
    }
    
    /**
     * Checks the key so that <b>null</b> becomes {@link #NULL_KEY}.
     * 
     * @param key the key to be checked
     * @return <code>key</code> if <code>key != <b>null</b></code>, {@link #NULL_KEY} else
     */
    private Object checkKey(Object key) {
        return key == null ? NULL_KEY : key;
    }


    @Override
    public void incrementValue(double value, Object key) {
        changeValueImpl(value, key, true);
    }

    @Override
    public void incrementValue(Double value, Object key) {
        if (null != value) {
            changeValueImpl(value, key, true);
        }
    }

    @Override
    public boolean isValueSet() {
        boolean def = !components.isEmpty();
        if (!def && null != links) {
            for (int l = 0; !def && l < links.size(); l++) {
                def = links.get(l).isValueSet();
            }
        }
        return def;
    }
    
    /**
     * Returns an iterable of the values.
     * 
     * @return the iterable of the values
     */
    protected Iterable<? extends AtomicDouble> values() {
        Iterable<? extends AtomicDouble> result;
        if (null == links) {
            result = components.values();
        } else {
            Map<Object, AtomicDouble> vals = new HashMap<Object, AtomicDouble>();
            for (int l = links.size() - 1; l >= 0; l--) {
                IObservation obs = links.get(l);
                for (Object key : obs.getComponentKeys()) {
                    AtomicDouble val = obs.getValue(key);
                    if (null != val) {
                        vals.put(key, val);
                    }
                }
            }
            vals.putAll(components);
            result = vals.values();
        }
        return result;
    }

    @Override
    public String toString() {
        String result = "{";
        Iterator<Map.Entry<Object, AtomicDouble>> iter = components.entrySet().iterator(); 
        Set<Object> done = new HashSet<Object>();
        while (iter.hasNext()) {
            Map.Entry<Object, AtomicDouble> ent = iter.next();
            Object key = ent.getKey();
            result += toString(key, ent.getValue());
            if (iter.hasNext()) {
                result += ", ";                
            }
            done.add(key);
        }
        if (null != links) {
            boolean emitComma = !done.isEmpty();
            for (int l = 0; l < links.size(); l++) {
                IObservation obs = links.get(l);
                for (Object key : obs.getComponentKeys()) {
                    if (!done.contains(key)) {
                        AtomicDouble val = obs.getValue(key);
                        if (null != val) {
                            if (emitComma) {
                                result += ", ";
                            }
                            result += toString(key, val);
                            done.add(key);
                            emitComma = true;
                        }
                    }
                }
            }
        }
        result += "}";
        return result;
    }

    /**
     * Turns a key-value pair into a string.
     * 
     * @param key the key
     * @param value the value
     * @return the resulting string
     */
    private String toString(Object key, AtomicDouble value) {
        String result;
        if (key == NULL_KEY) {
            result = "null";
        } else {
            result = key.toString();
        }
        result += "=";
        result += value;
        return result;
    }
    
    @Override
    public void clear() {
        components.clear();
        lastUpdate.set(-1);
        links = null;
    }
    
    @Override
    public AtomicDouble getValue(Object key) {
        AtomicDouble result = components.get(checkKey(key));
        if (null == result && null != links) {
            for (int l = 0; null == result && l < links.size(); l++) {
                result = links.get(l).getValue(key);
            }
        }
        return result;
    }
    
    @Override
    public long getLastUpdate() {
        return lastUpdate.get();
    }

    @Override
    public void setLastUpdate(long timestamp) {
        lastUpdate.set(timestamp);
    }

    @Override
    public int getComponentCount() {
        int result = components.size();
        if (null != links) {
            for (int l = 0; l < links.size(); l++) {
                result = links.get(l).getComponentCount();
            }
        }
        return result;
    }
    
    @Override
    public void clearComponents(Collection<Object> keys) {
        if (null != keys) {
            for (Object key : keys) {
                components.remove(key);
            }
            if (null != links) {
                for (int l = 0; l < links.size(); l++) {
                    links.get(l).clearComponents(keys);
                }
            }
        }
    }

    @Override
    public Set<Object> getComponentKeys() {
        Set<Object> result;
        if (null == links) {
            result = components.keySet();
        } else {
            result = new HashSet<Object>();
            result.addAll(components.keySet());
            for (int l = 0; l < links.size(); l++) {
                result.addAll(links.get(l).getComponentKeys());
            }
        }
        return result;
    }

    @Override
    public void link(IObservation observation) {
        if (null != observation && this != observation) {
            if (null == links) {
                links = new ArrayList<IObservation>();
            }
            if (!links.contains(observation)) {
                links.add(observation);
            }
        }
    }

    @Override
    public void unlink(IObservation observation) {
        if (null != links && this != observation) { 
            links.remove(observation);
            if (links.isEmpty()) {
                links = null;
            }
        }
    }

    @Override
    public boolean statisticsWhileReading() {
        return false;
    }

}
