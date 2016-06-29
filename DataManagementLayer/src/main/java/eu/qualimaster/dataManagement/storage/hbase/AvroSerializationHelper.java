package eu.qualimaster.dataManagement.storage.hbase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;

public class AvroSerializationHelper {
	private static DataFileWriter<Object> dataFileWriter;


	public static byte[] serializeToByte(Object testObject) throws IOException {
		Schema schema = ReflectData.AllowNull.get().getSchema(
				testObject.getClass());
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DatumWriter<Object> writer = new ReflectDatumWriter<Object>(
				Object.class);
		dataFileWriter = new DataFileWriter<Object>(writer);
		DataFileWriter<Object> out = dataFileWriter.setCodec(
				CodecFactory.deflateCodec(9)).create(schema, baos);
		out.append(testObject);
		out.flush();
		return baos.toByteArray();
	}

	public static Object deserializeFromByte(byte[] byteArray)
			throws IOException {
		SeekableByteArrayInput sin = new SeekableByteArrayInput(byteArray);
		System.out.println("length of read input stream " + sin.length());
		DatumReader<?> reader2 = new ReflectDatumReader<>();
		DataFileReader<?> in = new DataFileReader<>(sin, reader2);
		System.out.println(in.getSchema());
		System.out.println(in.hasNext());
		Object returnObject = null;
		System.out.println(in.getSchema().getFullName() );
		while (in.hasNext()) {
			returnObject = in.next();
			
		}
		in.close();
		return returnObject;
	}
}
