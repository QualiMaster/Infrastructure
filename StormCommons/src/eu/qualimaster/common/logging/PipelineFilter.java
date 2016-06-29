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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import eu.qualimaster.events.EventHandler;
import eu.qualimaster.events.EventManager;
import eu.qualimaster.logging.events.LoggingFilterEvent;

/**
 * A dynamic pipeline filter.
 * 
 * @author Holger Eichelberger
 */
class PipelineFilter extends Filter<ILoggingEvent> {

    private List<Pattern> filterClassNameRegExs = new ArrayList<Pattern>();
    
    /**
     * An event handler to adjust the filter dynamically.
     * 
     * @author Holger Eichelberger
     */
    private class FilterEventHandler extends EventHandler<LoggingFilterEvent> {

        /**
         * Creates an event handler.
         */
        protected FilterEventHandler() {
            super(LoggingFilterEvent.class);
        }

        @Override
        protected void handle(LoggingFilterEvent event) {
            List<Pattern> tmp = new ArrayList<Pattern>();
            synchronized (filterClassNameRegExs) {
                tmp.addAll(filterClassNameRegExs);
            }

            List<String> regExs = event.getFilterRemovals();
            if (null != regExs) {
                for (int i = 0; i < regExs.size(); i++) {
                    String regEx = regExs.get(i);
                    if (LoggingFilterEvent.REMOVE_ALL.equals(regEx)) {
                        tmp.clear();
                    } else if (null != regEx) {
                        for (int t = tmp.size() - 1; t > 0; t--) {
                            if (tmp.get(t).toString().equals(regEx)) {
                                tmp.remove(t);
                            }
                        }
                    }
                }
            }
            regExs = event.getFilterAdditions();
            if (null != regExs) {
                for (int i = 0; i < regExs.size(); i++) {
                    String regEx = regExs.get(i);
                    if (null != regEx) {
                        try {
                            Pattern pattern = Pattern.compile(regEx);
                            tmp.add(pattern);
                        } catch (PatternSyntaxException e) {
                        }
                    }
                }
            }
            synchronized (filterClassNameRegExs) {
                filterClassNameRegExs = tmp;
            }
        }
        
    }
    
    /**
     * Creates a pipeline filter and registers it as event handler for logging filter events.
     */
    PipelineFilter() {
        EventManager.register(new FilterEventHandler());
    }
    
    @Override
    public FilterReply decide(ILoggingEvent event) {
        synchronized (filterClassNameRegExs) {
            boolean found = false;
            if (!filterClassNameRegExs.isEmpty()) {
                StackTraceElement[] stack = event.getCallerData();
                for (int s = 0; !found && s < stack.length; s++) {
                    for (int p = 0; !found && p < filterClassNameRegExs.size(); p++) {
                        found = filterClassNameRegExs.get(p).matcher(stack[s].getClassName()).matches();
                    }
                }
            }
            return found ? FilterReply.ACCEPT : FilterReply.DENY;
        }
    }

}
