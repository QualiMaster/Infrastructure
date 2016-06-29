package eu.qualimaster.base.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.qualimaster.base.protos.ParameterProtos.SBooleanParameter;
import eu.qualimaster.base.protos.ParameterProtos.SIntegerParameter;
import eu.qualimaster.base.protos.ParameterProtos.SLongParameter;
import eu.qualimaster.base.protos.ParameterProtos.SRealParameter;
import eu.qualimaster.base.protos.ParameterProtos.SStringParameter;
import eu.qualimaster.base.serializer.Parameters.BooleanParameter;
import eu.qualimaster.base.serializer.Parameters.IntegerParameter;
import eu.qualimaster.base.serializer.Parameters.LongParameter;
import eu.qualimaster.base.serializer.Parameters.RealParameter;
import eu.qualimaster.base.serializer.Parameters.StringParameter;
import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;
/**
 * Define parameter serializers.
 * @author qin
 *
 */
public class ParameterSerializers {
    /**
     * Defines a serializer for the integer parameter.
     * @author qin
     *
     */
    public static class IntegerParameterSerializer implements ISerializer<IntegerParameter> {

        @Override
        public void serializeTo(IntegerParameter object, OutputStream out) throws IOException {
            SIntegerParameter tmp = SIntegerParameter.newBuilder()
                .setName(object.getName())
                .setValue(object.getValue())
                .build();
            tmp.writeDelimitedTo(out);
        }

        @Override
        public IntegerParameter deserializeFrom(InputStream in) throws IOException {
            SIntegerParameter tmp = SIntegerParameter.parseDelimitedFrom(in);
            Parameters.IntegerParameter parameter = new Parameters.IntegerParameter();
            parameter.setName(tmp.getName());
            parameter.setValue(tmp.getValue());
            return parameter;
        }

        @Override
        public void serializeTo(IntegerParameter object, IDataOutput out) throws IOException {
            out.writeString(object.getName());
            out.writeInt(object.getValue());
            
        }

        @Override
        public IntegerParameter deserializeFrom(IDataInput in) throws IOException {
            IntegerParameter parameter = new IntegerParameter();
            parameter.setName(in.nextString());
            parameter.setValue(in.nextInt());
            return parameter;
        }
        
    }
    /**
     * Defines a serializer for the string parameter.
     * @author qin
     *
     */
    public static class StringParameterSerializer implements ISerializer<StringParameter> {

        @Override
        public void serializeTo(StringParameter object, OutputStream out) throws IOException {
            SStringParameter tmp = SStringParameter.newBuilder()
                .setName(object.getName())
                .setValue(object.getValue())
                .build();
            tmp.writeDelimitedTo(out);
        }

        @Override
        public StringParameter deserializeFrom(InputStream in) throws IOException {
            SStringParameter tmp = SStringParameter.parseDelimitedFrom(in);
            StringParameter parameter = new StringParameter();
            parameter.setName(tmp.getName());
            parameter.setValue(tmp.getValue());
            return parameter;
        }
        
        @Override
        public void serializeTo(StringParameter object, IDataOutput out) throws IOException {
            out.writeString(object.getName());
            out.writeString(object.getValue());
            
        }

        @Override
        public StringParameter deserializeFrom(IDataInput in) throws IOException {
            StringParameter parameter = new StringParameter();
            parameter.setName(in.nextString());
            parameter.setValue(in.nextString());
            return parameter;
        }
        
    }
    /**
     * Defines a serializer for the boolean parameter.
     * @author qin
     *
     */
    public static class BooleanParameterSerializer implements ISerializer<BooleanParameter> {

        @Override
        public void serializeTo(BooleanParameter object, OutputStream out) throws IOException {
            SBooleanParameter tmp = SBooleanParameter.newBuilder()
                .setName(object.getName())
                .setValue(object.getValue())
                .build();
            tmp.writeDelimitedTo(out);
        }

        @Override
        public BooleanParameter deserializeFrom(InputStream in) throws IOException {
            SBooleanParameter tmp = SBooleanParameter.parseDelimitedFrom(in);
            BooleanParameter parameter = new BooleanParameter();
            parameter.setName(tmp.getName());
            parameter.setValue(tmp.getValue());
            return parameter;
        }

        @Override
        public void serializeTo(BooleanParameter object, IDataOutput out) throws IOException {
            out.writeString(object.getName());
            out.writeBoolean(object.getValue());
            
        }

        @Override
        public BooleanParameter deserializeFrom(IDataInput in) throws IOException {
            BooleanParameter parameter = new BooleanParameter();
            parameter.setName(in.nextString());
            parameter.setValue(in.nextBoolean());
            return parameter;
        }
    }
    /**
     * Defines a serializer for the long parameter.
     * @author qin
     *
     */
    public static class LongParameterSerializer implements ISerializer<LongParameter> {

        @Override
        public void serializeTo(LongParameter object, OutputStream out) throws IOException {
            SLongParameter tmp = SLongParameter.newBuilder()
                .setName(object.getName())
                .setValue(object.getValue())
                .build();
            tmp.writeDelimitedTo(out);
        }

        @Override
        public LongParameter deserializeFrom(InputStream in) throws IOException {
            SLongParameter tmp = SLongParameter.parseDelimitedFrom(in);
            LongParameter parameter = new LongParameter();
            parameter.setName(tmp.getName());
            parameter.setValue(tmp.getValue());
            return parameter;
        }
        
        @Override
        public void serializeTo(LongParameter object, IDataOutput out) throws IOException {
            out.writeString(object.getName());
            out.writeLong(object.getValue());
            
        }

        @Override
        public LongParameter deserializeFrom(IDataInput in) throws IOException {
            LongParameter parameter = new LongParameter();
            parameter.setName(in.nextString());
            parameter.setValue(in.nextLong());
            return parameter;
        }
    }
    /**
     * Defines a serializer for the real parameter.
     * @author qin
     *
     */
    public static class RealParameterSerializer implements ISerializer<RealParameter> {

        @Override
        public void serializeTo(RealParameter object, OutputStream out) throws IOException {
            SRealParameter tmp = SRealParameter.newBuilder()
                .setName(object.getName())
                .setValue(object.getValue())
                .build();
            tmp.writeDelimitedTo(out);
        }

        @Override
        public RealParameter deserializeFrom(InputStream in) throws IOException {
            SRealParameter tmp = SRealParameter.parseDelimitedFrom(in);
            RealParameter parameter = new RealParameter();
            parameter.setName(tmp.getName());
            parameter.setValue(tmp.getValue());
            return parameter;
        }

        @Override
        public void serializeTo(RealParameter object, IDataOutput out) throws IOException {
            out.writeString(object.getName());
            out.writeDouble(object.getValue());
            
        }

        @Override
        public RealParameter deserializeFrom(IDataInput in) throws IOException {
            RealParameter parameter = new RealParameter();
            parameter.setName(in.nextString());
            parameter.setValue(in.nextDouble());
            return parameter;
        }
        
    }
}
