package eu.qualimaster.dataManagement.common.replay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 
 * The schema of data elements to be put into and out from the replayStore
 * 
 * @author tuan
 * @since 30/05/2016
 *
 */
public class Tuple {
	
	/** Used to map to the corresponding table / schema in the ReplayStore */
	private String name;
	
	/** Assume that fields are read and written in the same order of their
	 * declaration */
	private List<Field> fields;

	public Tuple(String name) {
		this.name = name;
		this.fields = new ArrayList<>();
	}

	public Tuple(String name, Field... fields) {
		this.name = name;
		this.fields = Arrays.asList(fields);
	}

	/* Getter and setter methods */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Field> getFields() {
		return fields;
	}

	public void setFields(List<Field> fields) {
		this.fields = fields;
	}
	
	public Field getField(int index) {
		assert (fields != null && index < fields.size());
		return fields.get(index);
	}

	public void addField(Field field) {
		fields.add(field);
	}
}
