package eu.qualimaster.dataManagement.sinks.replay;

import eu.qualimaster.dataManagement.DataManager;
import eu.qualimaster.dataManagement.common.replay.Tuple;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;
import eu.qualimaster.dataManagement.serialization.SerializerRegistry;
import eu.qualimaster.dataManagement.storage.AbstractStorageTable;
import eu.qualimaster.dataManagement.storage.hbase.HBaseBatchStorageSupport;
import eu.qualimaster.dataManagement.storage.support.IStorageSupport;
import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;
import eu.qualimaster.dataManagement.strategies.NoStorageStrategyDescriptor;

import java.io.IOException;

import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The intercepter between the sink in the execution layer and the replay store
 * in the data management layer. It pushes the data into the replay store in the
 * asynchronous and bulk mode
 * 
 * @author tuan
 * @since 30/05/2016
 */
public class ReplayRecorder<T> {

	/** schema declaration to write to the store accordingly */
	// Note 08/06/16 - Tuan: We don't need to store these properties
	// private Tuple schema;

	/**
	 * Define the name of the processing unit for the replay mechanism Tuan
	 * 30/05/2016 - We might need to put this constant somewhere else, or re-use
	 * the name of the priority pipeline, as the Replay Mechanism is supposed to
	 * be demonstrated in that scenario
	 */
	private static final String STORE_UNIT = "replay-store";

	/**
	 * The in-memory data store to keep the ephemeral data to serve live query
	 */
	// Comment to test first the TSI store
	// private static final String IN_MEMORY_LOCATION = "redis-l3s";

	/**
	 * The permanent data store to save and aggregate data on a regular basis.
	 * Default value is HDFS
	 */
	// 2016-06-01 : Try first with HBase store
	// Note 08/06/16 - Tuan: We don't need to store these properties
	// private String location = "hdfs-l3s";
	// private String location = "hBase-l3s";

	/**
	 * We connect to two data stores, one in-memory and one durable, and write
	 * out data to both.
	 */
	private ReplayDataOutput output;
	private ISerializer<T> serializer;

	private static final Logger log = LoggerFactory.getLogger(ReplayRecorder.class);
	
	private Class<T> cls;

	public ReplayRecorder(Class<T> cls, Tuple schema, String location, IStorageStrategyDescriptor d) {
		this.cls = cls;
		//log.info("Replay: constructing ReplayRecorder");

		// The storage strategy is for in-memory, as
		// we never remove data in the permanent store
		if (d == null || d instanceof NoStorageStrategyDescriptor) {

			// Tuan 30/05/2016 TODO: Do we need to configure the default
			// capacity
			// or aging parameters ?
			// 30/05/2016 TODO: Why is this not the type of
			// IStorageStrategyDescriptor ?
			// d = new LeastRecentlyUsedStorageStrategyDescriptor(30, 10);
		}

		// Note 08/06/16 - Tuan: We don't need to store these properties
		// this.location = location;
		// this.schema = schema;

		AbstractStorageTable table = DataManager.REPLAY_STORAGE_MANAGER.getTable(location, schema.getName(), d);
		IStorageSupport storage = table.getStorageSupport();
		output = new ReplayDataOutput(schema, storage);
		this.serializer = getSerializer(cls);
		
	}

	/**
	 * Call by the replay sink in the asynchronous manner to push data into the
	 * replay store
	 */
	public void store(T data) throws IOException {
		//log.info("storing data = " + data.toString());
		if(serializer == null){
			//log.info("serializer is null");
			serializer = getSerializer(cls);
			//return;
		}

		serializer.serializeTo(data, output);
	}

	/**
     * Returns a serializer for <code>cls</code>.
     *
     * @param <T> the object type
     * @param cls the class to return the serializer for
     * @return the serializer or <b>null</b> of none was found
     */
    public static  <T> ISerializer<T> getSerializer(Class<T> cls) {
    	//LogManager.getLogger(SerializerRegistry.class).info(
        //        "get serializer instance: cls.getName() = "+ cls.getSimpleName());
		log.info("ReplayRecorder: Register with " + cls.getSimpleName() + ", " + cls.getName());
        return SerializerRegistry.getSerializer(cls.getSimpleName(), cls);    	
    }
	
	
	public void close() throws IOException {
		log.info("closing output");
		output.close();
	}
}
