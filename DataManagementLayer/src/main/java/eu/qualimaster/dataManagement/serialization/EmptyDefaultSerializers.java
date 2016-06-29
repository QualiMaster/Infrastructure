package eu.qualimaster.dataManagement.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class EmptyDefaultSerializers {

    public static class EmptyDefaultBasicTypeSerializer<T> implements ISerializer<T> {

	@Override
	public void serializeTo(T object, OutputStream out) throws IOException {
	    // does nothing
	}

	@Override
	public T deserializeFrom(InputStream in) throws IOException {
	    return null; //always null
	}

        @Override
        public void serializeTo(T object, IDataOutput out) throws IOException {
            // does nothing
        }
    
        @Override
        public T deserializeFrom(IDataInput in) throws IOException {
            return null; //always null
        }
	
    }
    
    public static class EmptyDefaultListTypeSerializer<T> implements ISerializer<List<T>> {
	
	@Override
	public void serializeTo(List<T> object, OutputStream out)
		throws IOException {
	    // does nothing
	}

	@Override
	public List<T> deserializeFrom(InputStream in) throws IOException {
	    return null; //always null
	}

        @Override
        public void serializeTo(List<T> object, IDataOutput out) throws IOException {
            // does nothing
        }
    
        @Override
        public List<T> deserializeFrom(IDataInput in) throws IOException {
            return null;
        }		
	
    }

}
