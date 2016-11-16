package eu.qualimaster.adaptation.internal;

import java.io.PrintStream;
import java.util.HashMap;

import eu.qualimaster.adaptation.events.AdaptationEvent;
import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.monitoring.events.FrozenSystemState;

/**
 * A log writer for adaptation log files.
 * 
 * @author Andrea Ceroni
 */
public class AdaptationLoggerFile implements IAdaptationLogger {

    /** The column headers.
     */
    private static final String[] COLUMN_NAMES = {"START_TIME", "END_TIME", "EVENT", "CONDITION", "STRATEGY",
        "STRATEGY_SUCCESS", "TACTIC", "TACTIC_SUCCESS", "MESSAGE", "ADAPTATION_SUCCESS" };

    /** To print to file.
     */
    private PrintStream out;

    /**
     * To handle multiple pending adaptations, which have not been enacted yet.
     */
    private HashMap<String, AdaptationUnit> adaptationsMap;

    /**
     * The current adaptation, filled in the methods between startAdaptation()
     * and endAdaptation(), which are sequential.
     */
    private AdaptationUnit currentAdaptation;

    /**
     * Creates the logger.
     * 
     * @param out the output print steam
     */
    public AdaptationLoggerFile(PrintStream out) {
        this.out = out;
        this.adaptationsMap = new HashMap<>();
        this.currentAdaptation = null;

        // print the header with the column names
        printHeader(COLUMN_NAMES);
    }

    @Override
    public void startAdaptation(AdaptationEvent event, FrozenSystemState state) {
        if(event == null || state == null){
            System.out.println("ERROR: null AdaptationEvent or FrozenSystemState" +
            		" in startAdaptation() method, could not write in the " +
            		"adaptation log.");
            return;
        }
        
        // create a new adaptation unit
        this.currentAdaptation = new AdaptationUnit();
        this.currentAdaptation.setStartTime(System.currentTimeMillis());
        this.currentAdaptation.setEvent(event.getClass().getSimpleName());

        // TODO missing: the condition/measurement that triggered the
        // adaptation, needed to derive success indicators
    }

    @Override
    public void executedStrategy(String name, boolean successful) {
        if(name == null){
            System.out.println("ERROR: null name in executedStrategy() method, " +
            		"could not write in the adaptation log.");
            return;
        }
        
        this.currentAdaptation.setStrategy(name);
        this.currentAdaptation.setStrategySuccess(successful);
    }

    @Override
    public void executedTactic(String name, boolean successful) {
        if(name == null){
            System.out.println("ERROR: null name in executedTactic() method, " +
                    "could not write in the adaptation log.");
            return;
        }
        this.currentAdaptation.setTactic(name);
        this.currentAdaptation.setTacticSuccess(successful);
    }

    @Override
    public void enacting(CoordinationCommand command) {
        if(command == null){
            System.out.println("ERROR: null CoordinationCommand in enacting()" +
            		" method, could not write in the adaptation log.");
            return;
        }
        this.currentAdaptation.setMessage(command.getMessageId());
    }

    @Override
    public void endAdaptation(boolean successful) {
        // add the current adaptation unit to the map, using the message id of
        // the coordination command as key
        AdaptationUnit pendingUnit = new AdaptationUnit(this.currentAdaptation);
        this.adaptationsMap.put(pendingUnit.getMessage(), pendingUnit);
        this.currentAdaptation = null;
    }

    @Override
    public void enacted(CoordinationCommand command, CoordinationCommandExecutionEvent event) {
        if(command == null || event == null){
            System.out.println("ERROR: null CoordinationCommand or CoordinationCommandExecutionEvent" +
                    " in enacted() method, could not write in the adaptation log.");
            return;
        }
        
        // get the corresponding adaptation unit via the message id
        Long endTime = System.currentTimeMillis();
        AdaptationUnit unit = this.adaptationsMap.get(command.getMessageId());
        if (unit == null) {
            System.out.println("ERROR: adaptation unit with message id " + command.getMessageId()
                            + " missing and could not be stored.");
            return;
        }

        // fill the missing fields
        unit.setEndTime(endTime);
        unit.setAdaptationSuccess(event.isSuccessful());

        // store the unit to file and remove it from the list of pending units
        storeAdaptationUnit(unit);
        this.adaptationsMap.remove(command.getMessageId());
    }

    /**
     * Prints a header.
     * 
     * @param names the names for the header
     */
    private void printHeader(String[] names) {
        for (int i = 0; i < names.length - 1; i++) {
            print(names[i]);
            printSeparator();
        }
        print(names[names.length - 1]);
        println();
    }

    /**
     * Stores an adaptation unit.
     * 
     * @param unit the unit
     */
    private void storeAdaptationUnit(AdaptationUnit unit) {
        print(unit.getStartTime());
        printSeparator();
        print(unit.getEndTime());
        printSeparator();
        print(unit.getEvent());
        printSeparator();
        print(unit.getCondition());
        printSeparator();
        print(unit.getStrategy());
        printSeparator();
        print(unit.isStrategySuccess());
        printSeparator();
        print(unit.getTactic());
        printSeparator();
        print(unit.isTacticSuccess());
        printSeparator();
        print(unit.getMessage());
        printSeparator();
        print(unit.isAdaptationSuccess());
        println();
    }

    /**
     * Prints a long value.
     * 
     * @param value
     *            the value to print
     */
    private void print(long value) {
        if (null != out) {
            out.print(value);
        }
    }

    // /**
    // * Prints a double value.
    // *
    // * @param value the value to print
    // */
    // private void print(double value) {
    // if (null != out) {
    // out.print(value);
    // }
    // }

    /**
     * Prints a text.
     * 
     * @param text
     *            the text to print
     */
    private void print(String text) {
        if (null != out) {
            out.print(text);
        }
    }

    /**
     * Prints a boolean.
     * 
     * @param bool
     *            the bool value to print
     */
    private void print(boolean bool) {
        if (null != out) {
            if (bool) {
                out.print("1");
            } else {
                out.print("0");
            }
        }
    }

    /**
     * Prints a tab separator.
     */
    private void printSeparator() {
        if (null != out) {
            out.print("\t");
        }
    }

    /**
     * Prints a new line.
     */
    private void println() {
        if (null != out) {
            out.println();
            out.flush();
        }
    }

    @Override
    public void close() {
        this.out.close();
        this.out = null;
        this.adaptationsMap.clear();
        this.currentAdaptation = null;
    }
    
}
