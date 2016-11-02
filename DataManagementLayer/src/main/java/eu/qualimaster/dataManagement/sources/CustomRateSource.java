package eu.qualimaster.dataManagement.sources;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;
import eu.qualimaster.dataManagement.strategies.NoStorageStrategyDescriptor;
import eu.qualimaster.observables.IObservable;

/**
 * A data source producing input with a custom rate. Override {@link #createData()} to produce the actual
 * data tuple-by-tuple and define the rate of the source via the constructor. Use {@link #getDataImpl()}
 * to obtain the next tuple for your source implementation.
 * 
 * @param <T> the type of the data
 *
 * @author Cui Qin, Holger Eichelberger, Andrea Ceroni
 */
public abstract class CustomRateSource <T> extends TimerTask implements IDataSource {
    
    private Queue<T> queue = new ConcurrentLinkedQueue<T>();
    private Timer timer;
    private int timerDelay;
    private int[] tupleCounts;
    private long beginDelay;
    private int step;

    /**
     * Creates a data source emitting data according to a vector of rates.
     * At each time point, a new element of the vector is used as rate.
     * 
     * @param rates the vector of rates (tuples per timerDelay ms)
     * @param timerDelay the period of the timer in ms (between 1 and 1000)
     */
    public CustomRateSource(int[] rates, int timerDelay) {
        this(0, rates, timerDelay);
    }
    
    /**
     * Creates a data source emitting data according to a vector of rates.
     * At each time point, a new element of the vector is used as rate.
     * 
     * @param beginDelay the delay in ms before creating any data
     * @param rates the vector of rates (tuples per timerDelay ms)
     * @param timerDelay the period of the timer in ms (between 1 and 1000)
     */
    public CustomRateSource(long beginDelay, int[] rates, int timerDelay) {
        if(timerDelay < 1 || timerDelay > 1000){
            System.out.println("ERROR: could not instantiate the source, " +
            		"timerDelay must be between 1 and 1000 ms.");
            return;
        }
        
        this.timerDelay = timerDelay;
        this.beginDelay = beginDelay;
        this.tupleCounts = new int[rates.length];
        for(int i = 0; i < rates.length; i++){
            this.tupleCounts[i] = rates[i];
        }
        this.step = 0;
    }

    @Override
    public void connect() {      
        if (null == timer) {
            timer = new Timer();
            timer.scheduleAtFixedRate(this, beginDelay, timerDelay);
        }
    }
    
    @Override
    public void disconnect() {       
        if (null != timer) {
            queue.clear();
            cancel();
            timer.cancel();
        }
    }

    /**
     * Returns the next tuple (the implementation of the related getData method).
     * 
     * @return the next tuple or <b>null</b> if there is none
     */
    protected T getDataImpl() {
        return queue.poll(); // null if queue is empty
    }
    
    /**
     * Creates a data tuple to be queued.
     * 
     * @return the tuple to e queued
     */
    protected abstract T createData();

    @Override
    public void run() {
        for (int i = 0; i < this.tupleCounts[this.step]; i++) {
            queue.offer(createData());
        }
        
        // increment the step
        this.step++;
        if(this.step >= this.tupleCounts.length){
            disconnect();
        }
    }
 
    @Override
    public void setStrategy(IStorageStrategyDescriptor strategy) {
        // ignore
    }

    @Override
    public IStorageStrategyDescriptor getStrategy() {
        return NoStorageStrategyDescriptor.INSTANCE;
    }

    @Override
    public Double getMeasurement(IObservable observable) {
        return null;
    }

}