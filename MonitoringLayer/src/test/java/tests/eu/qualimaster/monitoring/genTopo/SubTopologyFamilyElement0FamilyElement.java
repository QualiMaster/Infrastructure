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
package tests.eu.qualimaster.monitoring.genTopo;

/**
 * Implements the test family.
 * 
 * @author Holger Eichelberger
 */
public class SubTopologyFamilyElement0FamilyElement extends AbstractProcessor {

    private static final long serialVersionUID = 7654006005629709996L;

    /**
     * Creates the test family.
     * 
     * @param name the source name
     * @param namespace the namespace
     * @param sendMonitoringEvents whether monitoring events shall be sent out
     * @param sendRegular whether this monitor shall care for sending regular events (<code>true</code>) or 
     *     not (<code>false</code>, for thrift-based monitoring)
     */
    public SubTopologyFamilyElement0FamilyElement(String name, String namespace, boolean sendMonitoringEvents, 
        boolean sendRegular) {
        super(name, namespace, sendMonitoringEvents, sendRegular);
    }

}
