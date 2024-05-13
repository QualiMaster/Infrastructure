package switching.logging;

import java.io.PrintWriter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Provide a log writer writing the logs in parallel, i.e., 
 * logs are first pushed into a queue and then will be written into files.
 * @author Cui Qin
 *
 */
public class LogWriter implements Runnable {
    private PrintWriter out = null;
    private BlockingQueue<String> queue;
    private boolean cont = true;
    
    /**
     * Create a log writer instance.
     * @param out a <code>PrintWriter</code> instance used to write logs into files.
     */
    public LogWriter(PrintWriter out) {
        this.out = out;
        this.queue = new LinkedBlockingQueue<String>();
    }
    
    /**
     * Starts the server.
     */
    public void start() {
        if (null != out) {
            out.println("Starting the thread of the log writer.");
            out.flush();
        }
        new Thread(this).start();
    }
    
    @Override
    public void run() {
        while (cont) {
            if (null != out && !queue.isEmpty()) {
                out.println(queue.poll());
                out.flush();
            }
        }
    }
    
    /**
     * Pushes the log string into the queue.
     * @param logString the log string
     */
    public void pushLog(String logString) {
        try {
            queue.put(logString);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Stops the server.
     */
    public void stop() {
        if (null != out) {
            out.println("Stopping the log writer.");
            out.flush();
            while (!queue.isEmpty()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            out.close();
        }
        cont = false;
    }
}
