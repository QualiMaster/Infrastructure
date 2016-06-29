/*
 * Copyright 2009-2014 University of Hildesheim, Software Systems Engineering
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
package eu.qualimaster.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.logging.events.LoggingEvent;

/**
 * The QualiMaster logging appender to get information about the logging messages
 * and to do early filtering before sending the events through the infrastructure.
 * 
 * @author Holger Eichelberger
 */
public class QmAppender extends AppenderBase<ILoggingEvent> {

    private PipelineFilter filter;
    
    /**
     * Creates an instance and adds {@link PipelineFilter} as logging filter.
     */
    public QmAppender() { // TODO check whether this can be made private
        filter = new PipelineFilter();
        addFilter(filter);
    }
    
    @Override
    protected void append(ILoggingEvent event) {
        EventManager.send(new LoggingEvent(event.getTimeStamp(), 
            null == event.getLevel() ? "" : event.getLevel().toString(), 
            event.getFormattedMessage(), event.getThreadName()));
    }

}
