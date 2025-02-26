package eu.qualimaster.monitoring.volumePrediction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

/**
 * Class containing static methods to write/read volume data.
 * 
 * @author Andrea Ceroni
 */
public class DataUtils {
    /**
     * Reads volume data from a file. The usage of a TreeMap ensures the removal
     * of duplicate entries as well as the sorting of the values by date.
     * 
     * @param f the file containing volume data.
     * @return A TreeMap representing the data: keys are timestamps, values are
     *         volumes.
     */
    public static TreeMap<String, Long> readData(File f) {
        TreeMap<String, Long> data = new TreeMap<String, Long>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = reader.readLine()) != null
                    && line.compareTo("") != 0) {
                String[] fields = line.split(",");
                String date = fields[0];
                String time = fields[1];
                if (time.split(":").length == 2) {
                    time = time + ":00";
                }
                long dataValue = Long.valueOf(fields[fields.length - 1].trim());
                if (dataValue > Integer.MAX_VALUE) {
                    continue;
                    // dataValue = Integer.MAX_VALUE / 2000;
                }
                String[] dateFields = date.split("/");
                String year = dateFields[2];
                String month = dateFields[0];
                String day = dateFields[1];

                // avoid the lines with volume = 1 (containing the summary for
                // the given day)
                if (dataValue == 1)
                    continue;

                // use an hashmap to avoid more than one value for the same date
                // and time, also in case they are not adjacent
                data.put(year + "-" + month + "-" + day + "T" + time, dataValue);
            }
            reader.close();

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            return data;
        }
    }

    public static HashMap<String, ArrayList<Integer>> readStreamingData(
            String folder, HashSet<String> terms) {
        HashMap<String, ArrayList<Integer>> data = new HashMap<>();
        File dir = new File(folder);

        for (String term : terms) {
            File currFile = null;
            for (File f : dir.listFiles()) {
                if (f.getName().contains(term)) {
                    currFile = f;
                    break;
                }
            }

            if (currFile == null) {
                System.out.println("No streaming data available for term: "
                        + term);
                continue;
            }

            ArrayList<Integer> dataForTerm = readSingleColumnData(currFile);
            data.put(term, dataForTerm);
        }

        return data;
    }

    public static ArrayList<Integer> readSingleColumnData(File f) {
        ArrayList<Integer> data = new ArrayList<>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(f));
            String line = null;
            while ((line = reader.readLine()) != null) {
                int value = Integer.valueOf(line);
                data.add(value);
            }
            reader.close();

            return data;
        } catch (IOException e) {
            e.printStackTrace();
            return data;
        }
    }
}
