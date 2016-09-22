package tests.eu.qualimaster.storm;

import java.util.Map;

import tests.eu.qualimaster.storm.SignalCollector.SignalEntry;
import eu.qualimaster.base.algorithm.AbstractOutputItem;
import eu.qualimaster.base.algorithm.IDirectGroupingInfo;
import eu.qualimaster.common.signal.AlgorithmChangeSignal;
import eu.qualimaster.common.signal.BaseSignalBolt;
import eu.qualimaster.common.signal.ParameterChangeSignal;
import eu.qualimaster.common.signal.ShutdownSignal;
import eu.qualimaster.common.signal.SignalException;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineOptions;
import eu.qualimaster.monitoring.events.AlgorithmChangedMonitoringEvent;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

/**
 * A simple process bolt.
 * 
 * @author Holger Eichelberger
 */
@SuppressWarnings("serial")
public class Process extends BaseSignalBolt {

    private static SignalCollector signals = new SignalCollector(Naming.LOG_PROCESS);
    private transient IAlg alg;
    private transient AlgInput input;
    private transient AlgOutput output;
    private transient OutputCollector collector;
    private String[] shutdown;
    
    /**
     * Represents a reusable algorithm input.
     * 
     * @author Holger Eichelberger
     */
    private class AlgInput implements IAlg.IAlgInput {

        private Tuple tuple;

        /**
         * Initializes the underlying tuple.
         * 
         * @param tuple the tuple
         */
        public void init(Tuple tuple) {
            this.tuple = tuple;
        }
        
        @Override
        public int getValue() {
            return tuple.getInteger(0);
        }
        
    }
    
    /**
     * Represents an algorithm output.
     * 
     * @author Holger Eichelberger
     */
    private class AlgOutput extends AbstractOutputItem<AlgOutput> implements IAlg.IAlgOutput, IDirectGroupingInfo {

        private int value;
        private int taskId;

        /**
         * Initializes the values (reuse).
         */
        public void init() {
            value = -1;
        }
        
        @Override
        public void setValue(int value) {
            this.value = value;
        }
        
        /**
         * Returns the corresponding Storm values.
         * 
         * @return the Storm values
         */
        public Values getValues() {
            return new Values(value);
        }

        @Override
        public AlgOutput createItem() {
            return new AlgOutput();
        }

        @Override
        public void setTaskId(int taskId) {
            this.taskId = taskId;
        }

        @Override
        public int getTaskId() {
            return taskId;
        }
        
    }

    /**
     * Creates the "process" bolt.
     * 
     * @param elementName the element name
     * @param pipelineName the pipeline name
     * @param shutdown the other elements to shutdown (just name or pipeline/name)
     */
    public Process(String elementName, String pipelineName, String... shutdown) {
        super(elementName, pipelineName);
        this.shutdown = shutdown;
    }
    
    /**
     * Sends an algorithm change event and considers whether the coordination layer shall be bypassed for direct 
     * testing.
     * 
     * @param algorithm the new algorithm
     */
    private void sendAlgorithmChangeEvent(String algorithm) {
        EventManager.send(new AlgorithmChangedMonitoringEvent(getPipeline(), getName(), algorithm));
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
        super.prepare(stormConf, context, collector);
        this.collector = collector;
        input = new AlgInput();
        output = new AlgOutput();
        if (Naming.defaultInitializeAlgorithms(stormConf)) {
            alg = new Alg1();
            sendAlgorithmChangeEvent(Naming.NODE_PROCESS_ALG1);
        }
        System.out.println("PROCESS DELAY INIT " 
            + PipelineOptions.getExecutorIntArgument(stormConf, getName(), "delay", 0));
    }

    @Override
    public void execute(Tuple input) {
        startMonitoring();
        this.input.init(input);
        this.output.init();
        this.alg.process(this.input, this.output);
        Utils.sleep(300);
        Values values = this.output.getValues(); 
        collector.emit(values);
        collector.ack(input);
        endMonitoring();
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer declarer) {
        declarer.declare(new Fields("values"));
    }

    /**
     * Switches the algorithm based on a signal entry.
     * 
     * @param info the signal entry (may be <b>null</b>)
     */
    private void switchAlg(SignalEntry info) {
        if (null != info && null != info.getAlgorithm()) {
            String alg = info.getAlgorithm();
            if (Naming.NODE_PROCESS_ALG1.equals(alg)) {
                if (!(this.alg instanceof Alg1)) {
                    this.alg = new Alg1();
                    sendAlgorithmChangeEvent(alg);
                }
            } else if (Naming.NODE_PROCESS_ALG2.equals(alg)) {
                if (!(this.alg instanceof Alg2)) {
                    this.alg = new Alg2();
                    sendAlgorithmChangeEvent(alg);
                }
            }
        }
        
    }
    
    // testing
    
    /**
     * Returns the signal collector.
     * 
     * @return the signal collector
     */
    public SignalCollector getSignals() {
        return signals;
    }

    @Override
    public void notifyAlgorithmChange(AlgorithmChangeSignal signal) {
        switchAlg(signals.notifyAlgorithmChange(signal));
    }

    @Override
    public void notifyParameterChange(ParameterChangeSignal signal) {
        signals.notifyParameterChange(signal);
    }
    
    @Override
    protected void prepareShutdown(ShutdownSignal signal) {
        signals.notifyShutdown(signal);
        for (String s : shutdown) {
            String pipeline = getPipeline();
            String element = s;
            int pos = s.indexOf("/");
            if (pos > 0) {
                pipeline = s.substring(0, pos);
                element = s.substring(pos + 1);
            }
            send(new ShutdownSignal(pipeline, element));
        }
    }
    
    /**
     * Sends a shutdown signal.
     * 
     * @param signal the signal
     */
    private static void send(ShutdownSignal signal) {
        try {
            signal.sendSignal();
        } catch (SignalException e) {
            System.out.println(e.getMessage());
        }
    }

}
