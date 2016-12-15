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
 * 
 * @author qin
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
    @SuppressWarnings("rawtypes")
    public KryoSwitchTupleSerializer(final Map conf) {
        this.valuesSer = new KryoValuesSerializer(conf);
        this.valuesDeser = new KryoValuesDeserializer(conf);
        this.kryoIn = new Input(1);
        this.kryoOut = new Output(2000, 2000000000);
    }

    // checkstyle: stop exception type check

    @Override
    public byte[] serialize(ISwitchTuple tuple) {
        kryoOut.clear();
        try {
            kryoOut.writeLong(tuple.getId());
            valuesSer.serializeInto(tuple.getValues(), kryoOut);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            // catch KryoExceptions and whatever Storm serializers like to throw
            e.printStackTrace();
        }
        return kryoOut.toBytes();
    }

    @Override
    public ISwitchTuple deserialize(byte[] ser) {
        ISwitchTuple tuple;
        if (null != ser && ser.length > 0) {
            kryoIn.setBuffer(ser);
            try {
                tuple = new SwitchTuple(kryoIn.readLong(), valuesDeser.deserializeFrom(kryoIn));
            } catch (RuntimeException e) {
                // Buffer underflow? kryoIn.available() may solve the problem but consumes time
                // KryoException may be sufficient, Storm serializers like throwing plain RuntimeExceptions
                tuple = null;
            }
        } else {
            tuple = null;
        }
        return tuple;
    }

    // checkstyle: resume exception type check

}
