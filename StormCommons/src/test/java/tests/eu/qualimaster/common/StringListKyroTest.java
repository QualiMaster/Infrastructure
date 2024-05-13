package tests.eu.qualimaster.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import eu.qualimaster.base.algorithm.IFamily;
import eu.qualimaster.observables.IObservable;

/**
 * Tests the kyro serialization on string list.
 * 
 * @author qin
 *
 */
public class StringListKyroTest {
    /**
     * Defines the interface for the string list.
     */
    public interface IFStringList extends IFamily, Serializable {

        /**
         * Returns the string list value.
         * 
         * @return the string list value
         */
        public java.util.List<String> getStringList();

        /**
         * Changes the string list value.
         * 
         * @param stringList
         *            the string list
         */
        public void setStringList(java.util.List<String> stringList);
    }

    /**
     * Implementation of IFStringList interface.
     * 
     * @author qin
     *
     */
    @SuppressWarnings("serial")
    public class FStringList implements IFStringList {

        private List<String> stringList;

        @Override
        public void switchState(State state) {
        }

        @Override
        public Double getMeasurement(IObservable arg0) {
            return null;
        }

        @Override
        public List<String> getStringList() {
            return this.stringList;
        }

        @Override
        public void setStringList(List<String> stringList) {
            this.stringList = stringList;
        }

    }

    /**
     * Asserts that <code>item</code> is serializable with the given Kryo.
     * 
     * @param kryo
     *            the Kryo instance
     * @param item
     *            the item to be serialized / deserialized
     */
    @SuppressWarnings("unchecked")
    private void assertSerializable(Kryo kryo, IFStringList item) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Output out = new Output(buffer);
        kryo.writeObject(out, item.getStringList());
        out.close();

        byte[] ser = buffer.toByteArray();

        Input in = new Input(new ByteArrayInputStream(ser));
        IFStringList test = new FStringList();
        test.setStringList(kryo.readObject(in, ArrayList.class));
        in.close();
        System.out.println("item: " + item.getStringList() + ", test: " + test.getStringList());
        Assert.assertEquals(item.getStringList(), test.getStringList());
    }

    /**
     * Tests.
     */
    @Test
    public void test() {
        IFStringList item = new FStringList();
        List<String> listString = new ArrayList<String>();
        listString.add("a");
        listString.add("b");
        listString.add("c");
        item.setStringList(listString);

        Kryo k = StormTestUtils.createStormKryo();
        assertSerializable(k, item);
    }

}
