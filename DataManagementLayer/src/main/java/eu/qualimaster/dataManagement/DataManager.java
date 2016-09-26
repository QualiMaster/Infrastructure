package eu.qualimaster.dataManagement;

import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.dataManagement.common.AbstractDataManager;
import eu.qualimaster.dataManagement.events.ReferenceDataManagementEvent;
import eu.qualimaster.dataManagement.events.ShutdownEvent;
import eu.qualimaster.dataManagement.events.ReferenceDataManagementEvent.Command;
import eu.qualimaster.dataManagement.sinks.IDataSink;
import eu.qualimaster.dataManagement.sources.IDataSource;
import eu.qualimaster.dataManagement.storage.AbstractStorageManager;
import eu.qualimaster.dataManagement.storage.hbase.HBaseBatchStorageSupport;
import eu.qualimaster.dataManagement.storage.hbase.HBaseStorageTable;
import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.infrastructure.PipelineLifecycleEvent;

/**
 * Realizes the external interface of the storage manager.
 * 
 * @author Holger Eichelberger
 */
public class DataManager {

	/**
	 * The data source data manager.
	 */
	public static final DataSourceManager DATA_SOURCE_MANAGER = new DataSourceManager();

	/**
	 * The data sink data manager.
	 */
	public static final DataSinkManager DATA_SINK_MANAGER = new DataSinkManager();

	/**
	 * The intermediary processing data storage manager.
	 */
	public static final AbstractStorageManager<?> INTERMEDIARY_STORAGE_MANAGER = new HBaseStorageManager(
			"intermediary.");

	/**
	 * The intermediary processing data storage manager.
	 */
	public static final AbstractStorageManager<?> ADAPTATION_STORAGE_MANAGER = new HBaseStorageManager("adaptation.");

	/**
	 * The temporary data manager for replay mechanism - Default Redis-based
	 * key-value store
	 */
	public static final AbstractStorageManager<?> REPLAY_STORAGE_MANAGER = new ReplayStorageManager("replay.");

	/**
	 * The data manager to store aggregated source volumes (for source volume
	 * prediction).
	 */
	public static final AbstractStorageManager<?> VOLUME_PREDICTION_STORAGE_MANAGER = new HBaseStorageManager(
			"volumePrediction.");

	private static boolean started = false;
	private static boolean localMode = false;

	/**
	 * The handler for monitoring events.
	 * 
	 * @author Holger Eichelberger
	 */
	private static class PipelineLifecycleEventEventHandler extends EventHandler<PipelineLifecycleEvent> {

		/**
		 * Creates an adaptation event handler.
		 */
		protected PipelineLifecycleEventEventHandler() {
			super(PipelineLifecycleEvent.class);
		}

		@Override
		protected void handle(PipelineLifecycleEvent event) {
			String pipelineName = event.getPipeline();
			switch (event.getStatus()) {
			case STOPPING:
				disconnectAll(pipelineName); // see D5.2 stopping a pipeline
				discardAll(pipelineName);
				break;
			case INITIALIZED:
				connectAll(pipelineName);
				// TODO this is just a workaround and shall be removed as soon
				// as possible
				long additionalDelay = DataManagementConfiguration.getPipelineStartNotificationDelay();
				if (additionalDelay > 0) {
					try {
						Thread.sleep(additionalDelay);
					} catch (InterruptedException e) {
					}
				}
				// now we are able to work -> switch to started
				EventManager.handle(new PipelineLifecycleEvent(event, PipelineLifecycleEvent.Status.STARTED));
				break;
			default:
				break;
			}
		}

	}

	/**
	 * Handles a shutdown event.
	 * 
	 * @author Holger Eichelberger
	 */
	private static class ShutdownEventHandler extends EventHandler<ShutdownEvent> {

		protected ShutdownEventHandler() {
			super(ShutdownEvent.class);
		}

		@Override
		protected void handle(ShutdownEvent event) {
			DataManager.stop();
		}

	}

	/**
	 * Implements a data management event handler.
	 * 
	 * @author Holger Eichelberger
	 */
	private static class ReferenceDataManagementEventHandler extends EventHandler<ReferenceDataManagementEvent> {

		/**
		 * Creates a data management handler.
		 */
		protected ReferenceDataManagementEventHandler() {
			super(ReferenceDataManagementEvent.class);
		}

		@Override
		protected void handle(ReferenceDataManagementEvent event) {
			boolean relevant;
			Command cmd = event.getCommand();
			if (localMode) {
				// don't handle register/dispose as these shall be local
				// references
				relevant = Command.CONNECT == cmd || Command.DISCONNECT == cmd;
			} else {
				if (started) { // we are the central instance, handle only
								// register/dispose
					relevant = Command.REGISTER == cmd || Command.DISPOSE == cmd;
				} else { // we are on a Storm node, handle connect and
							// disconnect sent by central instance
					relevant = Command.CONNECT == cmd || Command.DISCONNECT == cmd;
				}
			}
			if (relevant) {
				String managerId = event.getManagerId();
				for (int m = 0; m < MANAGERS.length; m++) {
					AbstractDataManager<?> manager = MANAGERS[m];
					if (manager.getId().equals(managerId)) {
						manager.handle(event);
						break;
					}
				}
			}
		}

	}

	/**
	 * Defines the data sink manager type.
	 * 
	 * @author Holger Eichelberger
	 */
	public static class DataSinkManager extends AbstractDataManager<IDataSink> {

		/**
		 * Prevents external creation.
		 */
		private DataSinkManager() {
			super(IDataSink.class, "sinks");
		}

		/**
		 * Creates a data sink for <code>cls</code> and assigns it to
		 * <code>container</code>. Please note that multiple different data
		 * sources for one <code>cls</code> may be created, but the underlying
		 * factory may also just return individual proxies to the same physical
		 * data source (e.g., hidden behind a buffering mechanism).
		 * 
		 * @param <S>
		 *            the actual type to be created
		 * @param container
		 *            the (symbolic) name of a container to assign the created
		 *            instance to, no assignment will happen if
		 *            <code>container</code> is <b>null</b>, but then also
		 *            {@link #connectAll(String)},
		 *            {@link #disconnectAll(String)} and
		 *            {@link #discardAll(String)} will be useless.
		 * @param cls
		 *            the class of the data sink to be created
		 * @param strategy
		 *            the desired data storage strategy
		 * @return an instance of <code>cls</code> in the most easiest case, a
		 *         modified/wrapped instance of <code>cls</code> in case of
		 *         transparent data storage, <b>null</b> if no instance can be
		 *         created, e.g., in case that <code>cls</code> is an abstract
		 *         class or an interface
		 */
		public <S extends IDataSink> S createDataSink(String container, Class<S> cls,
				IStorageStrategyDescriptor strategy) {
			return create(container, cls, strategy);
		}
	}

	/**
	 * Defines the data source manager type.
	 * 
	 * @author Holger Eichelberger
	 */
	public static class DataSourceManager extends AbstractDataManager<IDataSource> {

		/**
		 * Prevents external creation.
		 */
		private DataSourceManager() {
			super(IDataSource.class, "sources");
		}

		/**
		 * Creates a data source for <code>cls</code> and assigns it to
		 * <code>container</code>. Please note that multiple different data
		 * sources for one <code>cls</code> may be created, but the underlying
		 * factory may also just return individual proxies to the same physical
		 * data source (e.g., hidden behind a buffering mechanism).
		 * 
		 * @param <S>
		 *            the actual type to be created
		 * @param container
		 *            the (symbolic) name of a container to assign the created
		 *            instance to, no assignment will happen if
		 *            <code>container</code> is <b>null</b>, but then also
		 *            {@link #connectAll(String)},
		 *            {@link #disconnectAll(String)} and
		 *            {@link #discardAll(String)} will be useless.
		 * @param cls
		 *            the class of the data source to be created
		 * @param strategy
		 *            the desired data storage strategy
		 * @return an instance of <code>cls</code> in the most easiest case, a
		 *         modified/wrapped instance of <code>cls</code> in case of
		 *         transparent data storage, <b>null</b> if no instance can be
		 *         created, e.g., in case that <code>cls</code> is an abstract
		 *         class or an interface
		 */
		public <S extends IDataSource> S createDataSource(String container, Class<S> cls,
				IStorageStrategyDescriptor strategy) {
			return create(container, cls, strategy);
		}
	}

	/**
	 * Defines the data source manager type.
	 * 
	 * @author Holger Eichelberger
	 */
	public static class HBaseStorageManager extends eu.qualimaster.dataManagement.storage.hbase.HBaseStorageManager {

		/**
		 * Prevents external creation.
		 * 
		 * @param tablePrefix
		 *            a fixed prefix internally prepended before each table name
		 */
		private HBaseStorageManager(String tablePrefix) {
			super(tablePrefix);
		}
	}

	/**
	 * Customize HBaseStorageManager and override Patrick's HBaseStorageSupport
	 * implementation in order to: - Allow bulk put into the table - Be able to
	 * extend to other efficient Time series DB like OpenTSDB in the future
	 * 
	 * @author Tuan
	 */
	public static class ReplayStorageManager extends eu.qualimaster.dataManagement.storage.hbase.HBaseStorageManager {

		/** Prevents external creation */
		protected ReplayStorageManager(String tablePrefix) {
			super(tablePrefix);
		}

		/** Use customized HBaseStorageSupport that allows bulk put */
		@Override
		protected HBaseStorageTable createTable(String tableName) {
			return new HBaseBatchStorageSupport(tableName);
		}
	}

	/**
	 * Register the event handler statically.
	 */
	static {
		EventManager.register(new PipelineLifecycleEventEventHandler());
		EventManager.register(new ReferenceDataManagementEventHandler());
		EventManager.register(new ShutdownEventHandler());
	}

	// enter new managers here!!!
	private static final AbstractDataManager<?>[] MANAGERS = { DATA_SOURCE_MANAGER, DATA_SINK_MANAGER,
			INTERMEDIARY_STORAGE_MANAGER, ADAPTATION_STORAGE_MANAGER, VOLUME_PREDICTION_STORAGE_MANAGER,
			REPLAY_STORAGE_MANAGER };

	/**
	 * Called as part of the startup process of the QualiMaster platform. Please
	 * call this method only on the central platform. For tests, see
	 * {@link #start(boolean)}.
	 */
	public static void start() {
		start(false);
	}

	/**
	 * Start the data management layer. Please call this method only on the
	 * central platform (with <code>testMode</code> set to <code>false</code> /
	 * via {@link #start()}) or, for tests, in the JVM of the local cluster
	 * (with <code>testMode</code> set to <code>true</code>).
	 * 
	 * @param testMode
	 *            run the data management layer in test mode
	 */
	public static void start(boolean testMode) {
		started = true;
		localMode = testMode;
	}

	/**
	 * Called as part of the shutdown process of the QualiMaster platform.
	 * Please call this method only on the central platform or, for tests, in
	 * the JVM of the local cluster.
	 */
	public static void stop() {
		started = false;
	}

	/**
	 * Returns whether the data management layer has been started.
	 * 
	 * @return <code>true</code> if started, <code>false</code> else
	 */
	public static boolean isStarted() {
		return started;
	}

	/**
	 * Determines the sequence of managers for startup (connect) or shutdown
	 * (disconnect). Actually, this works by partitioning all known managers in
	 * source, sink and other managers and recombining the sequence accordingly.
	 * 
	 * @param connect
	 *            whether the connect or disconnect sequence shall be determined
	 * @return the sequence to be processed
	 */
	private static List<AbstractDataManager<?>> determineSequence(boolean connect) {
		List<AbstractDataManager<?>> result = new ArrayList<AbstractDataManager<?>>();
		List<AbstractDataManager<?>> sourceManagers = new ArrayList<AbstractDataManager<?>>();
		List<AbstractDataManager<?>> sinkManagers = new ArrayList<AbstractDataManager<?>>();
		List<AbstractDataManager<?>> otherManagers = new ArrayList<AbstractDataManager<?>>();
		for (int m = 0; m < MANAGERS.length; m++) {
			AbstractDataManager<?> manager = MANAGERS[m];
			if (manager.managesDataSource()) {
				sourceManagers.add(manager);
			} else if (manager.managesDataSink()) {
				sinkManagers.add(manager);
			} else {
				otherManagers.add(manager);
			}
		}
		if (connect) {
			// on connect, sources shall be last to have the pipeline ready
			result.addAll(otherManagers);
			result.addAll(sinkManagers);
			result.addAll(sourceManagers);
		} else {
			// on disconnect, sources shall be first to avoid additional
			// incoming data
			result.addAll(sourceManagers);
			result.addAll(sinkManagers);
			result.addAll(otherManagers);
		}
		return result;
	}

	/**
	 * Disconnects all data elements known for the given <code>unit</code>.
	 * 
	 * @param unit
	 *            the unit (no operation if <b>null</b>)
	 */
	public static void disconnectAll(String unit) {
		if (null != unit) {
			for (AbstractDataManager<?> manager : determineSequence(false)) {
				manager.disconnectAll(unit);
			}
		}
	}

	/**
	 * Connects all data elements known for the given <code>unit</code>.
	 * 
	 * @param unit
	 *            the unit (no operation if <b>null</b>)
	 */
	public static void connectAll(String unit) {
		if (null != unit) {
			for (AbstractDataManager<?> manager : determineSequence(true)) {
				manager.connectAll(unit);
			}
		}
	}

	/**
	 * Discards all known elements for the given <code>unit</code>. Without
	 * creating further data elements, {@link #connectAll(String)} and
	 * {@link #disconnectAll(String)} will not perform any further operation.
	 * 
	 * @param unit
	 *            the container (no operation if <b>null</b>)
	 */
	public static void discardAll(String unit) {
		if (null != unit) {
			for (int m = 0; m < MANAGERS.length; m++) {
				MANAGERS[m].discardAll(unit);
			}
		}
	}

}
