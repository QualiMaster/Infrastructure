package eu.qualimaster.base.serializer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import eu.qualimaster.base.protos.StringListProtos.SStringList;
import eu.qualimaster.dataManagement.serialization.IDataInput;
import eu.qualimaster.dataManagement.serialization.IDataOutput;
import eu.qualimaster.dataManagement.serialization.ISerializer;

/**
 * Define a serializer for string list.
 * 
 * @author qin
 */
public class StringListSerializer implements ISerializer<List<String>> {

    @Override
    public void serializeTo(List<String> object, OutputStream out) throws IOException {
        SStringList tmp = SStringList.newBuilder().addAllString(object).build();
        tmp.writeDelimitedTo(out); 
    }

    @Override
    public List<String> deserializeFrom(InputStream in) throws IOException {
        SStringList tmp = SStringList.parseDelimitedFrom(in);
        List<String> result = tmp.getStringList();
        return result;
    }

    @Override
    public void serializeTo(List<String> object, IDataOutput out) throws IOException {
        if (null == object) {
            out.writeInt(-1);    
        } else {
            out.writeInt(object.size());
            for (int i = 0; i < object.size(); i++) {
                out.writeString(object.get(i));
            }
        }
    }

    @Override
    public List<String> deserializeFrom(IDataInput in) throws IOException {
        List<String> result;
        int size = in.nextInt();
        if (size == -1) {
            result = null;
        } else if (size < 0) {
            throw new IOException("negative size");
        }
        result = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            result.add(in.nextString());
        }
        return result;
    }

}
