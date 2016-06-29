package tests.eu.qualimaster.common;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.base.algorithm.AlgorithmUtils;

/**
 * Tests the utility functionalities of the algorithm.
 * @author qin
 *
 */
public class AlgorithmUtilsTest {

    /**
     * Tests the constructor searching.
     */
    @Test
    public void testConstructors() {
        Assert.assertTrue(AlgorithmUtils.findConstructor(MyClass.class));
        Assert.assertTrue(AlgorithmUtils.findConstructor(MyClass.class, Integer.class));
        Assert.assertTrue(AlgorithmUtils.findConstructor(MyClass.class, List.class));
        Assert.assertTrue(AlgorithmUtils.findConstructor(MyClass.class, List.class, List.class));
    }
    

}

/**
 * Creates a class for testing the constructor searching.
 * @author qin
 *
 */
class MyClass {
    /**
     * Constructor with a integer list argument.
     */
    public MyClass() {}

    /**
     * Constructor with a integer list argument.
     * @param arg the argument
     */
    public MyClass(int arg) {}
    /**
     * Constructor with a integer list argument.
     * @param arg the argument
     */
    public MyClass(List<Integer> arg) {}
    /**
     * Constructor with a integer list argument.
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    public MyClass(List<Integer> arg1, List<Integer> arg2) {}
}
