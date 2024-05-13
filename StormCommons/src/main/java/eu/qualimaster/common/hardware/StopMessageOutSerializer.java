package eu.qualimaster.common.hardware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.qualimaster.base.protos.UploadInterfaceProtos.SStopOut;
import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;

/**
 * Serializer for stop algorithm responses.
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
class StopMessageOutSerializer implements ISerializer<StopMessageOut> {

    @Override
    public void serializeTo(StopMessageOut msg, OutputStream out) throws IOException {
        SStopOut tmp = SStopOut.newBuilder()
            .setErrorMsg(msg.getErrorMsg())
            .build();
        tmp.writeDelimitedTo(out);
    }

    @Override
    public StopMessageOut deserializeFrom(InputStream in) throws IOException {
        StopMessageOut result = new StopMessageOut();
        SStopOut tmp = SStopOut.parseDelimitedFrom(in);
        result.setErrorMsg(tmp.getErrorMsg());
        return result;
    }

    @Override
    public void serializeTo(StopMessageOut object, IDataOutput out) throws IOException {
        out.writeString(object.getErrorMsg());
    }

    @Override
    public StopMessageOut deserializeFrom(IDataInput in) throws IOException {
        StopMessageOut result = new StopMessageOut();
        result.setErrorMsg(in.nextString());
        return result;
    }

}