package tests.eu.qualimaster.monitoring.profiling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import tests.eu.qualimaster.coordination.Utils;

/**
 * Collection of methods used by multiple tests in this package.
 * @author Christopher Voges
 *
 */
class TestTools {

    /**
     * Load test-data from file system. (testdata/kalman/performanceTestData)
     * @param sourceFile The name of the file to read.
     * @return {@link ArrayList} of {@link String}-lines contained in 'sourceFile' and are not beginning with '#'.
     */
    static ArrayList<String> loadData(String sourceFile) {
        ArrayList<String> testData = new ArrayList<>();
        File folder = new File(Utils.getTestdataDir(), "kalman");
        try {
            BufferedReader file = new BufferedReader(new FileReader(new File(folder, sourceFile)));
            try {
                String line;
                while ((line = file.readLine()) != null) {
                    line = line.trim();
                    // No comments or empty lines
                    if (!(line.startsWith("#")) && !(line.isEmpty())) {
                        testData.add(line);
                    }
                }
            } finally {
                file.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return testData;
    }

}
