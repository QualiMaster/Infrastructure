package tests.eu.qualimaster.common;

import java.io.IOException;
import java.util.Scanner;

import com.google.protobuf.ByteString;
import com.google.protobuf.TextFormat.ParseException;

import eu.qualimaster.common.hardware.HardwareControlConnection;
import eu.qualimaster.common.hardware.IHardwareDispatcher;
import eu.qualimaster.common.hardware.IsRunningAlgorithmOut;
import eu.qualimaster.common.hardware.StopMessageOut;
import eu.qualimaster.common.hardware.UploadMessageOut;

/**
 * A manual hardware control interface test.
 *
 * @author Gregory Chrysos
 * @author Pavlos Malakonakis
 * @author Evripides Sotiriadis
 * @author Holger Eichelberger
 */
public class ManualHardwareConnectionTest implements IHardwareDispatcher {

    private static final boolean ASYNCHRONOUS = true;
    
    /**
     * Sleeps for a given time.
     * 
     * @param time the time to sleep
     */
    private static void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Performs the test.
     * 
     * @param args ignored
     * @throws IOException in case of I/O problems
     * @throws ParseException in case of wrong user inputs
     */
    public static void main(String[] args) throws IOException, ParseException {
        final ManualHardwareConnectionTest dispatcher = new ManualHardwareConnectionTest();
        final HardwareControlConnection hwc = new HardwareControlConnection("147.27.39.13", 2400, 2401); //vergina
        new Thread(new Runnable() {
            @Override
            public void run() {
                Scanner in = new Scanner(System.in);
                int command = 0;
                while (true) {
                    sleep(1000);
                    System.out.println("Give Command: ");
                    command = in.nextInt(); //Read user command
                    try {
                        if (handleCommand(command, hwc, dispatcher)) {
                            in.close();
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        if (ASYNCHRONOUS) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    while (true) {
                        try {
                            hwc.receive(dispatcher, false);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }
    
    /**
     * Handles a command.
     * 
     * @param command the command number
     * @param hwc the hardware connection
     * @param dispatcher the dispatcher instance
     * @return whether the containing thread shall terminate
     * @throws IOException in case of I/O problems
     */
    private static boolean handleCommand(int command, HardwareControlConnection hwc, 
        IHardwareDispatcher dispatcher) throws IOException {
        boolean end = false;
        if (ASYNCHRONOUS) {
            if (command == 1) { // upload algorithm HY
                hwc.sendAlgorithmUpload("HY", ByteString.copyFromUtf8("hello"));
            } else if (command == 2) { // upload algorithm MI
                hwc.sendAlgorithmUpload("MI", ByteString.copyFromUtf8("hello"));
            } else if (command == 3) { // upload algorithm TE
                hwc.sendAlgorithmUpload("TE", ByteString.copyFromUtf8("hello"));
            } else if (command == 4) { // upload algorithm ECM
                hwc.sendAlgorithmUpload("ECM", ByteString.copyFromUtf8("hello"));
            } else if (command == 5) { // upload EH algorithm  (WRONG)
                hwc.sendAlgorithmUpload("EH", ByteString.copyFromUtf8("hello"));
            } else if (command == 6) { // stop HY algorithm
                hwc.sendStopAlgorithm("HY");
            } else if (command == 7) { // stop MI algorithm
                hwc.sendStopAlgorithm("MI");
            } else if (command == 8) { // stop TE algorithm
                hwc.sendStopAlgorithm("TE");
            } else if (command == 9) { // stop ECM algorithm
                hwc.sendStopAlgorithm("ECM");
            } else if (command == 10) { // stop EH algorithm  (WRONG)
                hwc.sendStopAlgorithm("EH");
            } else if (command == 11) { // Is HY algorithm running?
                hwc.sendIsRunning("HY");
            } else if (command == 12) { // Is MI algorithm running?
                hwc.sendIsRunning("MI");
            } else if (command == 13) { // Is TE algorithm running?
                hwc.sendIsRunning("TE");
            } else if (command == 14) { // Is ECM algorithm running?
                hwc.sendIsRunning("ECM");
            } else if (command == 15) { // Is EH algorithm running? (WRONG)
                hwc.sendIsRunning("EH");
            } else if (command == 16) { // Stop HW server
                hwc.sendStopServer();
                end = true;
            }
        } else {
            // cannot be in the same execution as synchronous as otherwise the receiver thread is catching up the
            // messages causing a deserialization exception for the synchronous side
            if (command == 1) {
                dispatcher.received(hwc.uploadAlgorithm("HY", ByteString.copyFromUtf8("hello")));
            } else if (command == 6) {
                System.out.println("STOP RESPONSE " + hwc.stopAlgorithm("HY"));
            } else if (command == 11) {
                System.out.println("IS RUNNING RESPONSE " + hwc.isRunning("HY"));
            }
        }
        return end;
    }

    @Override
    public void received(UploadMessageOut msg) {
        System.out.println("UPLOAD MESSAGE RESPONSE:");
        System.out.println(msg.getErrorMsg());
        System.out.println(msg.getPortIn());
        int portCount = msg.getPortOutCount();
        for (int p = 0; p < portCount; p++) {
            System.out.print(msg.getPortOut(p));
            if (p < portCount) {
                System.out.print(", ");
            }
        }
        System.out.println();
    }

    @Override
    public void received(IsRunningAlgorithmOut msg) {
        System.out.println("ISRUNNING MESSAGE RESPONSE:");
        System.out.println(msg.getIsRunning());
    }

    @Override
    public void received(StopMessageOut msg) {
        System.out.println("STOP MESSAGE RESPONSE:");
        System.out.println(msg.getErrorMsg());
    }

    @Override
    public void serverTerminated() {
        System.out.println("Done!");
        System.exit(0);
    }
}