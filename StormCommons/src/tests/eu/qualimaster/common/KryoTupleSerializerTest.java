package tests.eu.qualimaster.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import backtype.storm.Config;
import eu.qualimaster.base.algorithm.GeneralTuple;
import eu.qualimaster.base.algorithm.IGeneralTuple;
import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.algorithm.SwitchTuple;
import eu.qualimaster.base.serializer.IGeneralTupleSerializer;
import eu.qualimaster.base.serializer.ISwitchTupleSerializer;
import eu.qualimaster.base.serializer.KryoGeneralTupleSerializer;
import eu.qualimaster.base.serializer.KryoSwitchTupleSerializer;

/**
 * A test for kryo-based serializer for the switch tuple {@link ISwitchTuple} and 
 * the general tuple {@link IGeneralTuple}.
 * @author Cui Qin
 *
 */
public class KryoTupleSerializerTest {
    /**
     * Interface of the data item.
     * @author Cui Qin
     *
     */
    public static interface IDataItem extends Serializable {
        /**
         * Sets an id for the data item.
         * @param id the id
         */
        void setId(int id);
        /**
         * Returns an id from the data item.
         * @return the id 
         */
        int getId();
        /**
         * Sets the value for the data item.
         * @param value the value
         */
        void setValue(String value);
        /**
         * Returns the value from the data item.
         * @return the value
         */
        String getValue();
    }
    /**
     * Creates a data item.
     * @author Cui Qin
     *
     */
    public static class DataItem implements IDataItem {
        private int id;
        private String value;
        /**
         * Creates a data item.
         * @param id the id
         * @param value the value
         */
        public DataItem(int id, String value) {
            this.id = id;
            this.value = value;
        }
        
        @Override
        public void setId(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }
    } 
    /**
     * Kryo serializer for the IDataItem.
     * @author Cui Qin
     *
     */
    public static class DataItemSerializer extends Serializer<DataItem> {        
        @Override
        public void write(Kryo kryo, Output output, DataItem object) {
            output.writeInt(object.getId());
            output.writeString(object.getValue());
        }

        @Override
        public DataItem read(Kryo kryo, Input input, Class<DataItem> type) {
            DataItem result = new DataItem(input.readInt(), input.readString());
            return result;
        }

    }
    
    /**
     * Test.
     */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Test
    public void test() {
        //create a data item
        DataItem dataItem = new DataItem(1, "data");
        List<Object> tupleValues = new ArrayList<Object>();
        tupleValues.add(dataItem);
        
        //create a general tuple
        IGeneralTuple generalTuple = new GeneralTuple(tupleValues);
        //asserts the type
        Assert.assertEquals(true, generalTuple.isGeneralTuple());
        
        //create a switch tuple
        ISwitchTuple switchTuple = new SwitchTuple(1, tupleValues);
        //asserts the type
        Assert.assertEquals(false, switchTuple.isGeneralTuple());
        
        //get the storm kryo map
        Map conf = StormTestUtils.createStormKryoConf();
        //register the custom serializer
        Config.registerSerialization(conf, DataItem.class, DataItemSerializer.class);
        
        //serialize a general tuple
        KryoGeneralTupleSerializer genSer = new KryoGeneralTupleSerializer(conf);        
        assertSerialization(dataItem, generalTuple, genSer);
        
        //serialize a switch tuple
        KryoSwitchTupleSerializer swiSer = new KryoSwitchTupleSerializer(conf);        
        assertSerialization(dataItem, switchTuple, swiSer);

    }
    
    /**
     * Asserts the serialization for a general tuple.
     * @param expected the expected data
     * @param tuple the general tuple to be serialized
     * @param serializer the general serializer
     */
    @SuppressWarnings({ "deprecation", "unused" })
    private void assertSerialization(IDataItem expected, IGeneralTuple tuple, IGeneralTupleSerializer serializer) {
        byte[] byteSer = serializer.serialize(tuple);
        IGeneralTuple result = serializer.deserialize(byteSer);
        IDataItem revData = (IDataItem) result.getValue(0);
        Assert.assertEquals(expected.getId(), revData.getId());
        Assert.assertEquals(expected.getValue(), revData.getValue());
    }
    
    /**
     * Asserts the serialization for a switch tuple.
     * @param expected the expected data
     * @param tuple the switch tuple to be serialized
     * @param serializer the switch serializer
     */
    @SuppressWarnings({ "deprecation", "unused" })
    private void assertSerialization(IDataItem expected, ISwitchTuple tuple, ISwitchTupleSerializer serializer) {
        byte[] byteSer = serializer.serialize(tuple);
        ISwitchTuple result = serializer.deserialize(byteSer);
        IDataItem revData = (IDataItem) result.getValue(0);
        Assert.assertEquals(expected.getId(), revData.getId());
        Assert.assertEquals(expected.getValue(), revData.getValue());
    }

}
