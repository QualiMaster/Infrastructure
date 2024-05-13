package eu.qualimaster.common.hardware;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import eu.qualimaster.base.protos.UploadInterfaceProtos.SIsRunningOut;
import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;

/**
 * Serializer for is-running responses.
 * 
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 */
class IsRunningAlgorithmOutSerializer implements ISerializer<IsRunningAlgorithmOut> {

    @Override
    public void serializeTo(IsRunningAlgorithmOut msg, OutputStream out) throws IOException {
        SIsRunningOut tmp = SIsRunningOut.newBuilder()
            .setIsRunning(msg.getIsRunning())
            .build();
        tmp.writeDelimitedTo(out);
    }

    @Override
    public IsRunningAlgorithmOut deserializeFrom(InputStream in) throws IOException {
        IsRunningAlgorithmOut result = new IsRunningAlgorithmOut();
        SIsRunningOut tmp = SIsRunningOut.parseDelimitedFrom(in);
        result.setIsRunning(tmp.getIsRunning());
        return result;
    }

    @Override
    public void serializeTo(IsRunningAlgorithmOut object, IDataOutput out) throws IOException {
        out.writeBoolean(object.getIsRunning());
    }

    @Override
    public IsRunningAlgorithmOut deserializeFrom(IDataInput in) throws IOException {
        IsRunningAlgorithmOut result = new IsRunningAlgorithmOut();
        result.setIsRunning(in.nextBoolean());
        return result;
    }
}
