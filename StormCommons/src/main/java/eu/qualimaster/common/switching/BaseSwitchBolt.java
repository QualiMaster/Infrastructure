package eu.qualimaster.common.switching;

import java.io.PrintWriter;
import java.util.Map;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import eu.qualimaster.common.logging.DataLogger;
import eu.qualimaster.common.signal.BaseSignalBolt;

/**
 * Implements a basic switching Bolt, carrying the common parts for all
 * switching-related bolts.
 * 
 * @author Cui Qin
 *
 */
@SuppressWarnings("serial")
public abstract class BaseSwitchBolt extends BaseSignalBolt {
	private transient PrintWriter logWriter = null;

	/**
	 * Creates a switch Bolt.
	 * 
	 * @param name        the name of the Bolt
	 * @param pipeline    the pipeline, namely the name of the pipeline which the
	 *                    Bolt belongs to
	 * @param sendRegular whether this monitor shall care for sending regular events
	 *                    (<code>true</code>) or not (<code>false</code>, for
	 *                    thrift-based monitoring)
	 */
	public BaseSwitchBolt(String name, String pipeline, boolean sendRegular) {
		super(name, pipeline, sendRegular);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public void prepare(Map conf, TopologyContext context, OutputCollector collector) {
		super.prepare(conf, context, collector);
		String logDir = (String) conf.get("LOG.DIRECTORY");
//        logWriter = new LogWriter(DataLogger.getPrintWriter(logDir + getName() + ".log"));
		logWriter = DataLogger.getPrintWriter(logDir + getName() + ".log");
	}

	/**
	 * Adds the switch actions.
	 */
	public void addSwitchActions() {
	}


//    /**
//     * Returns the log writer.
//     * @return the log writer
//     */
//    protected LogWriter getLogWriter() {
//        return logWriter;
//    }

	/**
	 * Returns the log writer.
	 * 
	 * @return the log writer
	 */
	protected PrintWriter getLogWriter() {
		return logWriter;
	}
}
