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
package eu.qualimaster.events;

import eu.qualimaster.common.QMInternal;

/**
 * Sent out if requested by event clients on a regular basis.
 * 
 * @author Holger Eichelberger
 * @see AbstractTimerEventHandler
 */
@QMInternal
public final class TimerEvent implements ILocalEvent {

    public static final String CHANNEL = "Timer";
    public static final TimerEvent INSTANCE = new TimerEvent();
    private static final long serialVersionUID = 8273219102585092332L;

    /**
     * Prevents external creation.
     */
    private TimerEvent() {
    }
    
    @Override
    public String getChannel() {
        return CHANNEL;
    }

}
