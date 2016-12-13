package eu.qualimaster.dataManagement.sources;

import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;
import eu.qualimaster.dataManagement.strategies.NoStorageStrategyDescriptor;
import eu.qualimaster.observables.IObservable;

/**
 * A simple fixed-rate data source. Override {@link #createData()} to produce the actual
 * data tuple-by-tuple and define the rate of the source via the constructor. Use {@link #getDataImpl()}
 * to obtain the next tuple for your source implementation.
 * 
 * @param <T> the type of the data
 *
 * @author Cui Qin, Holger Eichelberger
 */
public abstract class FixedRateSource <T> extends TimerTask implements IDataSource {
    
    private Queue<T> queue = new ConcurrentLinkedQueue<T>();
    private Timer timer;
    private int timerDelay;
    private int tupleCount;
    private long beginDelay;

    /**
     * Creates a data source emitting data at a defined rate.
     * 
     * @param rate the rate, i.e., tuples per second
     */
    public FixedRateSource(int rate) {
        this(0, rate);
    }
    
    /**
     * Creates a data source emitting data at a defined rate.
     * 
     * @param beginDelay the delay in ms before creating any data
     * @param rate the rate, i.e., tuples per second
     */
    public FixedRateSource(long beginDelay, int rate) {
        // 4 / sec -> 1000, 4
        // 40 / sec -> 100, 4
        // 100 / sec -> 10, 1
        // 1000 / sec -> 1, 1
        // 10000 / sec -> 1, 10
    System.out.println("FixedRateSource - " + rate + " items/s.");
	this.beginDelay = beginDelay;
        timerDelay = 1000; // max timer delay = 1 sec
        int tmp = rate;
        while (tmp >= 10 && timerDelay > 1) {
            timerDelay /= 10;
            tmp /= 10;
        }
        tupleCount = Math.max(1, rate / (1000 / timerDelay));
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
        for (int i = 0; i < tupleCount; i++) {
            queue.offer(createData());
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