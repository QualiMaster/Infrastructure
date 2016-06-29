package eu.qualimaster.common.hardware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.qualimaster.base.protos.UploadInterfaceProtos.SIsRunningIn;
import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;

/**
 * Serializer for is-running requests.
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
class IsRunningAlgorithmInSerializer implements ISerializer<IsRunningAlgorithmIn> {

    @Override
    public void serializeTo(IsRunningAlgorithmIn msg, OutputStream out) throws IOException {
        SIsRunningIn tmp = SIsRunningIn.newBuilder()
            .setId(msg.getId())
            .build();
        tmp.writeDelimitedTo(out);
    }

    @Override
    public IsRunningAlgorithmIn deserializeFrom(InputStream in) throws IOException {
        IsRunningAlgorithmIn result = new IsRunningAlgorithmIn();
        SIsRunningIn tmp = SIsRunningIn.parseDelimitedFrom(in);
        result.setId(tmp.getId());
        return result;
    }

    @Override
    public void serializeTo(IsRunningAlgorithmIn object, IDataOutput out) throws IOException {
        out.writeString(object.getId());
    }

    @Override
    public IsRunningAlgorithmIn deserializeFrom(IDataInput in) throws IOException {
        IsRunningAlgorithmIn result = new IsRunningAlgorithmIn();
        result.setId(in.nextString());
        return result;
    }

}