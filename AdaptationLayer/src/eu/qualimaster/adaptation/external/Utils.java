package eu.qualimaster.adaptation.external;

/**
 * Some message utility methods.
 * 
 * @author Holger Eichelberger
 */
public class Utils {

    /**
     * Prevents external creation (utility).
     */
    private Utils() {
    }
   
    /**
     * Returns whether two objects are equal considering <b>null</b>.
     * 
     * @param o1 the first object
     * @param o2 the second object
     * @return if <code>o1</code> equals <code>o2</code> or whether both objects are <b>null</b>
     */
    public static boolean equals(Object o1, Object o2) {
        return ((null == o1 && null == o2) || (null != o1 && o1.equals(o2)));
    }
    
    /**
     * Returns the hashcode of <code>obj</code> considering that <code>obj</code> may be <b>null</b>.
     * 
     * @param obj the obj to return the hashcode for
     * @return the hashcode
     */
    public static int hashCode(Object obj) {
        int result;
        if (null == obj) {
            result = 0;
        } else {
            result = obj.hashCode();
        }
        return result;
    }
    
    /**
     * Returns the hash code of the given boolean.
     * 
     * @param bool the boolean
     * @return the hash code
     */
    static int hashCode(boolean bool) {
        return bool ? 1 : 0;
    }

}
