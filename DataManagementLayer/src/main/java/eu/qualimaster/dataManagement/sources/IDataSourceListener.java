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
package eu.qualimaster.dataManagement.sources;

import java.io.Serializable;

/**
 * Callback for components holding data sources so that a data source can inform the holder about certain changes.
 * A data source listener must be serializable as it is typically part of the state of the holder.
 * 
 * @author Holger Eichelberger
 */
public interface IDataSourceListener extends Serializable {
    
    /**
     * Notifies the holder about a change of {@link IDataSource#getIdsNamesMap()}.
     */
    public void notifyIdsNamesMapChanged();

}
