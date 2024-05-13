package eu.qualimaster.base.serializer;

import java.io.IOException;
import java.util.Map;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import backtype.storm.serialization.KryoValuesDeserializer;
import backtype.storm.serialization.KryoValuesSerializer;
import eu.qualimaster.base.algorithm.GeneralTuple;
import eu.qualimaster.base.algorithm.IGeneralTuple;

/**
 * Implementation of the kryo serializer of the general Tuple {@link IGeneralTupleSerializer}.
 * 
 * @author Cui Qin
 */
public class KryoGeneralTupleSerializer implements IGeneralTupleSerializer {
    private KryoValuesSerializer valuesSer;
    private KryoValuesDeserializer valuesDeser;
    private Output kryoOut;
    private Input kryoIn;
    /**
     * Creates the kryo serializer for the general tuple.
     * @param conf the Storm configuration file
     */
    @SuppressWarnings("rawtypes")
    public KryoGeneralTupleSerializer(final Map conf) {
        this.valuesSer = new KryoValuesSerializer(conf);
        this.valuesDeser = new KryoValuesDeserializer(conf);
        this.kryoIn = new Input(1);
        this.kryoOut = new Output(2000, 2000000000);
    }

    // checkstyle: stop exception type check
    
    @Override
    public byte[] serialize(IGeneralTuple tuple) {
        kryoOut.clear();
        try {
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
    public IGeneralTuple deserialize(byte[] ser) {
        IGeneralTuple tuple;
        if (null != ser && ser.length > 0) {
            kryoIn.setBuffer(ser);
            try {
                tuple = new GeneralTuple(valuesDeser.deserializeFrom(kryoIn));
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
