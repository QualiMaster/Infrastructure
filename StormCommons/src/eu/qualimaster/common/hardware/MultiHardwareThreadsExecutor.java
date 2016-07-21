package eu.qualimaster.common.hardware;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Concurrent executor service for creating multiple hardware connections.
 * 
 * @author Cui Qin
 *
 */
public class MultiHardwareThreadsExecutor {
    private int threadsNum = 0;
    private List<Runnable> threadList = new ArrayList<Runnable>();
    private Map<String, List<Integer>> servers;
    private Class<?> handler;
    private ExecutorService executor;
    
    /**
     * Creates a executor for multiple hardware connections.
     * @param servers the hardware servers to be connected with
     * @param handler the hardware thread handler
     * @param threadsNum the number of threads to be created
     */
    public MultiHardwareThreadsExecutor(Map<String, List<Integer>> servers, Class<?> handler, int threadsNum) {
        this.servers = servers;
        this.handler = handler;
        this.threadsNum = threadsNum;
    }
    
    /**
     * Creates multiple hardware threads.
     */
    public void createMultiThreads() {
        executor = Executors.newFixedThreadPool(threadsNum);
        String ip;
        int port;
        List<Integer> ports;
        Iterator<String> it = servers.keySet().iterator();
        while (it.hasNext()) {
            ip = it.next();
            ports = servers.get(ip);
            for (int i = 0; i < ports.size(); i++) {
                port = ports.get(i);
                Runnable thread;
                try {
                    thread = (Runnable) handler.getDeclaredConstructor(String.class, int.class).newInstance(ip, port);
                    threadList.add(thread);
                    executor.execute(thread);
                } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Shuts down the executor service.
     */
    public void shutDown() {
        executor.shutdown();
    }
    
    /**
     * Checks whether all tasks are terminated.
     * @return true if all tasks have completed following shut down
     */
    public boolean isTerminated() {
        return executor.isTerminated();
    }
    
    /**
     * Gets all thread instances.
     * @return all thread instances
     */
    public List<Runnable> getThreadList() {
        return this.threadList;
    }
}
