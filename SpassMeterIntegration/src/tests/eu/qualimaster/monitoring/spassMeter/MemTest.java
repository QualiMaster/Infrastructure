package tests.eu.qualimaster.monitoring.spassMeter;

/**
 * Performs memory allocation testing.
 * 
 * @author Holger Eichelberger
 */
public class MemTest {

    @SuppressWarnings("unused")
    private static Data data;
    
    /**
     * Defines the maximum allocation count.
     */
    private static final int MAX_ALLOC = 10000;

    /**
     * Performs the allocation testing.
     */
    static void doTest() {
        for (int i = 0; i < MAX_ALLOC; i++) {
            data = new Data();
            if (i % 50 == 0) {
                System.gc();
            }
            if (0 == i % 100) {
                System.out.print(".");
            }
        }
    }
    
    /**
     * For calling it as a program.
     * 
     * @param args ignored
     */
    public static void main(String[] args) {
        doTest();
    }

}
