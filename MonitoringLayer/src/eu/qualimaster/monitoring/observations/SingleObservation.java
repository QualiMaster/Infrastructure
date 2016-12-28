package eu.qualimaster.monitoring.observations;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implements a single observation.
 * 
 * @author Holger Eichelberger
 */
public class SingleObservation implements IObservation {

    private static final long serialVersionUID = 3792522026731325940L;
    private static final Set<Object> KEYS = createDefaultKeys();
                    
    private double initial;
    private AtomicDouble value = new AtomicDouble();
    private AtomicLong firstUpdate = new AtomicLong(-1);
    private AtomicLong lastUpdate = new AtomicLong(-1);
    
    /**
     * Creates a single observation with initial value 0. Without further
     * setting values, the result of {@link #isValueSet()} will be <code>false</code>.
     */
    public SingleObservation() {
        this(0.0);
    }

    /**
     * Creates a single observation with a given initial. Without further
     * setting values, the result of {@link #isValueSet()} will be <code>false</code>.
     * 
     * @param value the initial value
     */
    public SingleObservation(double value) {
        this.initial = value;
        clear();
    }

    /**
     * Creates a new single observation from a given single observation.
     * 
     * @param source the source observation
     */
    protected SingleObservation(SingleObservation source) {
        this.initial = source.initial;
        this.value.set(source.value.get());
        this.lastUpdate.set(source.lastUpdate.get());
        this.firstUpdate.set(source.firstUpdate.get());
    }
    
    /**
     * Creates the default keys collection.
     * 
     * @return the default keys collection (unmodifiable)
     */
    private static final Set<Object> createDefaultKeys() {
        Set<Object> result = new HashSet<Object>();
        result.add(AbstractCompoundObservation.NULL_KEY);
        return Collections.unmodifiableSet(result);
    }
    
    @Override
    public boolean isComposite() {
        return false;
    }

    @Override
    public void setValue(Double value, Object key) {
        if (null != value) {
            setValue(value.doubleValue(), key);
        }
    }

    @Override
    public void incrementValue(Double value, Object key) {
        if (null != value) {
            incrementValue(value.doubleValue(), key);
        } 
    }
    
    @Override
    public void setValue(double value, Object key) {
        this.value.set(value);
        update();
    }

    @Override
    public void incrementValue(double value, Object key) {
        this.value.addAndGet(value);
        update();
    }
    
    /**
     * Updates the base values.
     */
    private void update() {
        long now = System.currentTimeMillis();
        if (firstUpdate.get() < 0) {
            firstUpdate.set(now);
        }
        lastUpdate.set(now);
    }

    @Override
    public double getValue() {
        return value.get();
    }
    
    @Override
    public double getLocalValue() {
        return value.get();
    }

    @Override
    public AtomicDouble getValue(Object key) {
        return value;
    }
    
    @Override
    public boolean isValueSet() {
        return lastUpdate.get() > 0;
    }

    @Override
    public String toString() {
        String result;
        if (isValueSet()) {
            result = value.toString();
        } else {
            result = "-";
        }
        return result;
    }

    @Override
    public void clear() {
        value.set(initial);
        lastUpdate.set(-1);
        firstUpdate.set(-1);
    }

    @Override
    public IObservation copy(IObservationProvider provider) {
        return new SingleObservation(this);
    }
    
    @Override
    public long getLastUpdate() {
        return lastUpdate.get();
    }
    
    @Override
    public long getFirstUpdate() {
        return firstUpdate.get();
    }
    
    @Override
    public void setLastUpdate(long timestamp) {
        lastUpdate.set(timestamp);
    }

    @Override
    public int getComponentCount() {
        return 1;
    }

    @Override
    public void clearComponents(Collection<Object> keys) {
        // nothing to do
    }
    
    @Override
    public Set<Object> getComponentKeys() {
        return KEYS;
    }

    @Override
    public void link(IObservation observation) {
        // nothing to do
    }

    @Override
    public void unlink(IObservation observation) {
        // nothing to do
    }
    
    @Override
    public int getLinkCount() {
        return 0;
    }

    @Override
    public IObservation getLink(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public boolean statisticsWhileReading() {
        return false;
    }

    @Override
    public void switchedTo(boolean direct) {
        if (!direct) {
            clear();
        }
    }

}
