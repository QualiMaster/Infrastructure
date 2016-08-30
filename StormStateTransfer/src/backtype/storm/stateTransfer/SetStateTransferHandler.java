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
package backtype.storm.stateTransfer;

import java.lang.reflect.Field;
import java.util.Set;

/**
 * Implements a state transfer handler for sets.
 * 
 * @author Holger Eichelberger
 */
@SuppressWarnings("rawtypes")
public class SetStateTransferHandler extends StateTransferHandler<Set> {

    /**
     * Creates the state transfer handler.
     */
    protected SetStateTransferHandler() {
        super(Set.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean doStateTransfer(PartOfState annotation, Field field, Object target, Set oldValue, Set newValue)
        throws SecurityException, IllegalArgumentException, IllegalAccessException {
        Boolean recurse = null;
        StateHandlingStrategy strategy = getStrategy(annotation);
        Set revisedNewValue = newValue;
        if (null != oldValue && null != newValue) {
            switch (strategy) {
            case CLEAR_AND_FILL:
                oldValue.clear();
                oldValue.addAll(newValue);
                revisedNewValue = oldValue;
                recurse = false;
                break;
            case MERGE:
            case MERGE_AND_KEEP_OLD: // Set type is caring for that
                oldValue.addAll(newValue);
                revisedNewValue = oldValue;
                recurse = false;
                break;
            default:
                break;
            }
        }
        boolean result = doDefaultObjectStateTransfer(annotation, field, target, oldValue, revisedNewValue);
        if (null != recurse) {
            result = recurse;
        }
        return result;
    }

}
