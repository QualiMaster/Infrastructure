/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.coordination.commands;

import java.util.ArrayList;
import java.util.List;

/**
 * Is called when a command is executed.
 * 
 * @author Holger Eichelberger
 */
public class CoordinationCommandNotifier {

    private static List<ICoordinationCommandNotifier> notifiers = new ArrayList<ICoordinationCommandNotifier>();
    
    /**
     * A pluggable command notifier.
     * 
     * @author Holger Eichelberger
     */
    public interface ICoordinationCommandNotifier {

        /**
         * Notifies about a sent command. The command now contains
         * information about the local event manager client id and the message id.
         * 
         * @param command the sent command
         */
        public void notifySent(CoordinationCommand command);
        
    }
    
    /**
     * Changes the command notifier instance.
     * 
     * @param notifier the notifier instance to use (ignored if <b>null</b>)
     */
    public static void addNotifier(ICoordinationCommandNotifier notifier) {
        if (null != notifier && !notifiers.contains(notifier)) {
            notifiers.add(notifier);
        }
    }

    /**
     * Removes a notifier instance.
     * 
     * @param notifier the notifier instance to remove (ignored if <b>null</b>)
     */
    public static void removeNotifier(ICoordinationCommandNotifier notifier) {
        if (null != notifier) {
            notifiers.remove(notifier);
        }
    }

    /**
     * Notifies about a sent command. The command now contains
     * information about the local event manager client id and the message id.
     * 
     * @param command the sent command
     * @see ICoordinationCommandNotifier#notifySent(CoordinationCommand)
     */
    public static void notifySent(CoordinationCommand command) {
        for (int i = 0; i < notifiers.size(); i++) {
            notifiers.get(i).notifySent(command);
        }
    }

}
