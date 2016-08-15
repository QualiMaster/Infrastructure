package tests.eu.qualimaster.monitoring.profiling;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.profiling.Kalman;


/**
 * Here multiple time-lines are given. For these the prediction-quality when
 * predicting one time-step ahead is checked against previously calculated
 * minimal criteria.
 * 
 * @author Christopher Voges
 *
 */
public class QualityTest {

    /**
     * Conducting a quality benchmark for multiple different input-sets.
     * For each input-set it is measured how big the average relative absolute prediction error is.
     * Afterwards this value is compared to a given maximum. 
     * Should it exceed the maximum, the test fails.
     * The test fails, when a run exceeds the given time.
     */
    @Test
    public void testAgainstHistory() {
        ArrayList<String> testData = TestTools.loadData("qualityTestData");
        for (String string : testData) {
            // Only numbers and the separation-signs ('.', ',' and ';') are allowed
            if (string.matches("[0-9;\\.,]+")) {
                String[] data = string.replaceAll(",", ".").split(";");
                double[] entries = new double[data.length];
                for (int i = 0; i < data.length; i++) {
                    try {
                        entries[i] = new Double(data[i]);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
                Kalman filter = new Kalman();
                // The first and second entry define the part of the time-set for which the quality is checked
                int start = (int) entries[0];
                int end = (int) entries[1];
                // The third entry contains the allowed mean error in %
                double meanErrorAllowed = entries[2];
                double meanError = 0;
                // Going through the measurements
                for (int i = 0 + 3; i < entries.length - 1; i++) {
                    // Calculating the predictions
                    filter.update(i, entries[i]);
                    double predicted = filter.predict();
                    // Sum mean error, when in relevant section
                    if (i >= start + 2 && i < end + 2) {
                        meanError += (entries[i] == 0) ? 0 
                            : Math.abs((predicted / entries[i]) - 1);
                    }
                }
                // Setting the error-sum in relation
                meanError = (entries.length > 1) ? meanError / (end - start) : 0;
                
                Assert.assertTrue((meanError * 100) <= meanErrorAllowed);
                
            } else {
                System.err.print("Following line was skipped for containing illegal characters: ");
                System.err.println(string);
            }
        }
    }
}