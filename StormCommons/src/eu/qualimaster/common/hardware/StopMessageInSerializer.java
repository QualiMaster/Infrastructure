package eu.qualimaster.common.hardware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.qualimaster.base.protos.UploadInterfaceProtos.SStopIn;
import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;

/**
 * Serializer for stop algorithm requests.
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
class StopMessageInSerializer implements ISerializer<StopMessageIn> {

    @Override
    public void serializeTo(StopMessageIn msg, OutputStream out) throws IOException {
        SStopIn tmp = SStopIn.newBuilder()
            .setId(msg.getId())
            .build();
        tmp.writeDelimitedTo(out);
    }

    @Override
    public StopMessageIn deserializeFrom(InputStream in) throws IOException {
        StopMessageIn result = new StopMessageIn();
        SStopIn tmp = SStopIn.parseDelimitedFrom(in);
        result.setId(tmp.getId());
        return result;
    }

    @Override
    public void serializeTo(StopMessageIn object, IDataOutput out) throws IOException {
        out.writeString(object.getId());
    }

    @Override
    public StopMessageIn deserializeFrom(IDataInput in) throws IOException {
        StopMessageIn result = new StopMessageIn();
        result.setId(in.nextString());
        return result;
    }

}
