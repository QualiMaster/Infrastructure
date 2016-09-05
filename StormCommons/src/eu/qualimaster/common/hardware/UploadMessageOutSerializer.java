package eu.qualimaster.common.hardware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.qualimaster.base.protos.UploadInterfaceProtos.SUploadOut;
import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;

/**
 * Serializer for outgoing upload responses.
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
class UploadMessageOutSerializer implements ISerializer<UploadMessageOut> {

    @Override
    public void serializeTo(UploadMessageOut msg, OutputStream out) throws IOException {
        SUploadOut.Builder builder = SUploadOut.newBuilder()
            .setErrorMsg(msg.getErrorMsg())
            .setPortIn(msg.getPortIn());
        for (int p = 0; p < msg.getPortOutCount(); p++) {
            builder.addPortOut(msg.getPortOut(p));
        }
        SUploadOut tmp = builder.build();
        tmp.writeDelimitedTo(out);
    }

    @Override
    public UploadMessageOut deserializeFrom(InputStream in) throws IOException {
        UploadMessageOut result = new UploadMessageOut();
        SUploadOut tmp = SUploadOut.parseDelimitedFrom(in);
        result.setErrorMsg(tmp.getErrorMsg());
        result.setPortIn(tmp.getPortIn());
        int pCount = tmp.getPortOutCount();
        int[] ports = new int[pCount];
        for (int i = 0; i < pCount; i++) {
            ports[i] = tmp.getPortOut(i);
        }
        result.setPortsOut(ports);
        return result;
    }

    @Override
    public void serializeTo(UploadMessageOut object, IDataOutput out) throws IOException {
        out.writeString(object.getErrorMsg());
        out.writeInt(object.getPortIn());
        out.writeIntArray(object.getPortsOut());
    }

    @Override
    public UploadMessageOut deserializeFrom(IDataInput in) throws IOException {
        UploadMessageOut result = new UploadMessageOut();
        result.setErrorMsg(in.nextString());
        result.setPortIn(in.nextInt());
        result.setPortsOut(in.nextIntArray());
        return result;
    }

}
