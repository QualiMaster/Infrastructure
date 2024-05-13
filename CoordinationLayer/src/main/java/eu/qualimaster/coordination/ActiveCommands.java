/*
 * Copyright 2009-2016 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.coordination;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import eu.qualimaster.coordination.commands.CoordinationCommand;
import eu.qualimaster.coordination.events.CoordinationCommandExecutionEvent;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.monitoring.events.IEnactmentCompletedMonitoringEvent;

/**
 * Represents a set of active commands to be deactivated / acknowledged by enactment events.
 * Instances of this class are stored and updated upon execution.
 * 
 * @author Holger Eichelberger
 *
 */
class ActiveCommands {

    private String causeMessageId;
    private CoordinationCommand cmd;
    private long time = System.currentTimeMillis();
    private Map<String, CoordinationCommand> cmds = Collections.synchronizedMap(
        new HashMap<String, CoordinationCommand>());
    
    /**
     * Creates an active commands object from <code>cmd</code>.
     * 
     * @param cmd the command to create the commands object from
     */
    ActiveCommands(CoordinationCommand cmd) {
        this.cmd = cmd;
        this.causeMessageId = cmd.getMessageId() + "-" + cmd.getSenderId();
        this.causeMessageId = this.causeMessageId.replace(":", "-"); // just to be sure for the signals
        EnactmentCommandCollector collector = new EnactmentCommandCollector();
        cmd.accept(collector);
        for (CoordinationCommand c : collector.getResult()) {
            String signature = EnactmentSignatureProvider.getSignature(c);
            if (null != signature) {
                cmds.put(signature, c);
            }
        }
    }
    
    /**
     * Returns the (combined) cause message id used in execution system signalling.
     * 
     * @return the cause message id
     */
    public String getCauseMessageId() {
        return causeMessageId;
    }
    
    /**
     * Returns whether the represented command set has been completely processed.
     * 
     * @return <code>true</code> if processed completely, <code>false</code> else
     */
    public boolean isEmpty() {
        return cmds.isEmpty();
    }
    
    /**
     * Checks whether <code>event</code> contains the signature of a corresponding coordination
     * command that can be considered as completed (removed).
     * 
     * @param event the event
     */
    void checkRemove(IEnactmentCompletedMonitoringEvent event) {
        String signature = EnactmentSignatureProvider.getSignature(event);
        if (null != signature) {
            cmds.remove(signature);
        }
        if (isEmpty()) {
            time = System.currentTimeMillis() - time;
            EventManager.send(new CoordinationCommandExecutionEvent(cmd, null, 
                CoordinationExecutionCode.SUCCESSFUL, ""));
        }
    }
    
    /**
     * Is called if this instance shall be removed from its store due to a timeout.
     */
    void notifyTimeout() {
        if (!isEmpty()) {
            EnactmentCommandCollector collector = new EnactmentCommandCollector();
            cmd.accept(collector);
            for (CoordinationCommand c : collector.getResult()) {
                String signature = EnactmentSignatureProvider.getSignature(c);
                if (null != signature) {
                    if (cmds.containsKey(signature)) {
                        EventManager.send(new CoordinationCommandExecutionEvent(cmd, c, 
                            CoordinationExecutionCode.RESPONSE_TIMEOUT, "Enactment response timeout"));
                        //CoordinationManager.execute(c); // for resending 
                        break;
                    }
                }            
            }
        }
    }
    
    /**
     * Returns the timestamp of starting the processing.
     * 
     * @return the timestamp
     */
    public long getEnactmentTime() {
        long result;
        if (!isEmpty()) {
            result = System.currentTimeMillis() - time;
        } else {
            result = time;
        }
        return result;
    }
    
}
