package eu.qualimaster.base.serializer;

import java.io.IOException;
import java.util.Map;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import backtype.storm.serialization.KryoValuesDeserializer;
import backtype.storm.serialization.KryoValuesSerializer;
import eu.qualimaster.base.algorithm.ISwitchTuple;
import eu.qualimaster.base.algorithm.SwitchTuple;

/**
 * Implementation of the serializer for the switch tuple.
 * @author qin
 *
 */
public class KryoSwitchTupleSerializer implements ISwitchTupleSerializer {
    private KryoValuesSerializer valuesSer;
    private KryoValuesDeserializer valuesDeser;
    private Output kryoOut;
    private Input kryoIn;
    
    /**
     * Creates the kryo serializer for the switch tuple.
     * @param conf the storm config
     */
    public KryoSwitchTupleSerializer(final Map conf) {
        this.valuesSer = new KryoValuesSerializer(conf);
        this.valuesDeser = new KryoValuesDeserializer(conf);
        this.kryoIn = new Input(1);
        this.kryoOut = new Output(2000, 2000000000);
    }
    
    @Override
    public byte[] serialize(ISwitchTuple tuple) {
        kryoOut.clear();
        kryoOut.writeInt(tuple.getId());
        try {
            valuesSer.serializeInto(tuple.getValues(), kryoOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return kryoOut.toBytes();
    }

    @Override
    public ISwitchTuple deserialize(byte[] ser) {
        kryoIn.setBuffer(ser);
        ISwitchTuple tuple = new SwitchTuple(kryoIn.readInt(), valuesDeser.deserializeFrom(kryoIn));
        return tuple;
    }

}
