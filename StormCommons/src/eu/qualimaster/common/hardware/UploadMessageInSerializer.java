package eu.qualimaster.common.hardware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.google.protobuf.ByteString;

import eu.qualimaster.base.protos.UploadInterfaceProtos.SUploadIn;
import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;

/**
 * Serializer for received upload requests.
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
class UploadMessageInSerializer implements ISerializer<UploadMessageIn> {

    @Override
    public void serializeTo(UploadMessageIn msg, OutputStream out) throws IOException {
        SUploadIn tmp = SUploadIn.newBuilder()
            .setId(msg.getId())
            .setExecutableCode(msg.getExecutable())
            .build();
        tmp.writeDelimitedTo(out);
    }

    @Override
    public UploadMessageIn deserializeFrom(InputStream in) throws IOException {
        UploadMessageIn result = new UploadMessageIn();
        SUploadIn tmp = SUploadIn.parseDelimitedFrom(in);
        result.setId(tmp.getId());
        result.setExecutable(tmp.getExecutableCode());
        return result;
    }

    @Override
    public void serializeTo(UploadMessageIn object, IDataOutput out) throws IOException {
        out.writeString(object.getId());
        out.writeByteArray(object.getExecutable().toByteArray());
    }

    @Override
    public UploadMessageIn deserializeFrom(IDataInput in) throws IOException {
        UploadMessageIn result = new UploadMessageIn();
        result.setId(in.nextString());
        result.setExecutable(ByteString.copyFrom(in.nextByteArray()));
        return result;
    }
}
