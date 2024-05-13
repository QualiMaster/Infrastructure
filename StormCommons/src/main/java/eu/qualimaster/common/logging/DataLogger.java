package eu.qualimaster.common.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import eu.qualimaster.file.Utils;

/**
 * Provide a data logger for writing logs into a file.
 * @author Cui Qin
 *
 */
public class DataLogger {
    /**
     * Returns a print writer to write the log in a given location.
     * @param filePath the full path of the file including the postfix ".log".
     * @return a print writer
     */
    public static PrintWriter getPrintWriter(String filePath) {
        PrintWriter writer = null;
        Writer fileOut = null;
        try {
            File file = new File(filePath);
            file.setReadable(true, false);
            file.setWritable(true, false);
//            fileOut = Utils.createFileOutputStream(file, false);
            writer = new PrintWriter(new BufferedWriter(new FileWriter(file)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
			e.printStackTrace();
		}
        return writer;
    }
    
    /**
     * Returns a print writer to write the log in a given location.
     * @param fileName the log name
     * @param directory the location to write the log
     * @return a print writer
     */
    public static PrintWriter getPrintWriter(String directory, String fileName) {
        PrintWriter writer = null;
        FileOutputStream fileOut = null;
        try {
            File file = new File(directory);
            if (!file.exists()) {
                String userhome = System.getProperty("user.home");
                file = new File(userhome);
            }
            File logFile = new File(file, fileName);
            logFile.setReadable(true, false);
            logFile.setWritable(true, false);
            fileOut = Utils.createFileOutputStream(logFile, false);
            writer = new PrintWriter(fileOut);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return writer;
    }
}
