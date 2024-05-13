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
package eu.qualimaster.monitoring.hardware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.LogManager;

import eu.qualimaster.coordination.RepositoryConnector;
import eu.qualimaster.coordination.RepositoryConnector.Models;
import eu.qualimaster.coordination.RepositoryConnector.Phase;
import eu.qualimaster.monitoring.MonitoringConfiguration;
import net.ssehub.easy.varModel.confModel.CompoundVariable;
import net.ssehub.easy.varModel.confModel.Configuration;
import net.ssehub.easy.varModel.confModel.ContainerVariable;
import net.ssehub.easy.varModel.confModel.IDecisionVariable;
import net.ssehub.easy.varModel.model.DecisionVariableDeclaration;
import net.ssehub.easy.varModel.model.ModelQuery;
import net.ssehub.easy.varModel.model.ModelQueryException;
import net.ssehub.easy.varModel.model.Project;

/**
 * Provides access to hardware-specific information stored in the current configuration. Actually, instantiating
 * this information into the infrastructure configurtion file would be more correct, but let's do it for now in
 * a reflective way.
 * 
 * @author Holger Eichelberger
 */
public class HardwareConfigurationAccess {

    /**
     * Auxiliary information about the number of available DFEs. Value is an Integer.
     */
    public static final String AUX_DFES = "DFEs";

    /**
     * Information required to monitor hardware.
     * 
     * @author Holger Eichelberger
     */
    public static class HardwareMonitoringInfo {

        private String name;
        private String host;
        private int port;
        private Map<String, Object> aux;
        
        /**
         * Creates a monitoring information object.
         * 
         * @param name the name of the hardware cluster
         * @param host the host name to send monitoring requests to
         * @param port the monitoring port
         * @param aux auxiliary information
         */
        public HardwareMonitoringInfo(String name, String host, int port, Map<String, Object> aux) {
            this.host = host;
            this.port = port;
            this.aux = aux;
            this.name = name;
        }

        /**
         * Returns the hardware cluster name.
         * 
         * @return the hardware cluster  name
         */
        public String getName() {
            return name;
        }
        
        /**
         * Returns the host name.
         * 
         * @return the host name
         */
        public String getHost() {
            return host;
        }

        /**
         * Returns the monitoring port.
         * 
         * @return the monitoring port
         */
        public int getPort() {
            return port;
        }
        
        /**
         * Returns auxiliary information.
         * 
         * @param key the key to the information
         * @return the auxiliary information (may be <b>null</b> if none)
         */
        public Object getAux(String key) {
            return null == aux ? null : aux.get(key);
        }
        
        /**
         * Returns an auxiliary hardware information value.
         * 
         * @param auxKey the auxiliary key
         * @return the auxiliary value, may be <b>null</b> if undefined
         */
        public Integer getAuxInteger(String auxKey) {
            Integer result = null;
            Object value = getAux(auxKey);
            if (value instanceof Number) {
                result = ((Number) value).intValue();
            }
            return result;
        }

        /**
         * Returns an auxiliary hardware information value.
         * 
         * @param auxKey the auxiliary key
         * @param undef the value to be returned if the auxiliary value is not defined
         * @return the auxiliary value, may be <b>null</b> if undefined
         */
        public int getAuxInt(String auxKey, int undef) {
            Integer value = getAuxInteger(auxKey);
            return null == value ? undef : value.intValue();
        }
        
        @Override
        public String toString() {
            return name + " @" + host + ":" + port + " aux:" + aux;
        }
        
    }

    /**
     * Returns the relevant information for monitoring from the configuration.
     * 
     * @return the hardware monitoring information, empty also in case of errors
     */
    public static HardwareMonitoringInfo[] getHardwareClusterInfo() {
        HardwareMonitoringInfo[] result = null;
        Models models = RepositoryConnector.getModels(Phase.MONITORING);
        if (null != models) {
            models.startUsing();
            try {
                Configuration configuration = models.getConfiguration();
                if (null != configuration) { // fallback case without model
                    Project qm = configuration.getProject();
                    DecisionVariableDeclaration decl = (DecisionVariableDeclaration) 
                        ModelQuery.findElementByName(qm, "clusters", DecisionVariableDeclaration.class);
                    if (null != decl) {
                        IDecisionVariable var = configuration.getDecision(decl);
                        if (var instanceof ContainerVariable) {
                            result = obtainHardwareClusterInfo(configuration, (ContainerVariable) var);
                        }
                    }
                }
            } catch (ModelQueryException e) {
                LogManager.getLogger(HardwareConfigurationAccess.class).error(e.getMessage());
            }
            models.endUsing();
        } else {
            LogManager.getLogger(HardwareConfigurationAccess.class).error(
                "Monitoring infrastructure model not loaded - cannot monitor hardware");
        }
        if (null == result) {
            // convenience
            result = new HardwareMonitoringInfo[0];
        }
        return result;
    }
    
    /**
     * Obtains the hardware cluster information from the respective container variable.
     * 
     * @param clusters the clusters variable from the configuration
     * @param configuration the actual configuration
     * @return the hardware monitoring information, <b>null</b> if none was found
     */
    private static HardwareMonitoringInfo[] obtainHardwareClusterInfo(Configuration configuration, 
        ContainerVariable clusters) {
        Set<String> filter = MonitoringConfiguration.getMonitoringHardwareFilter();
        ArrayList<HardwareMonitoringInfo> tmp = new ArrayList<HardwareMonitoringInfo>();
        for (int c = 0; c < clusters.getNestedElementsCount(); c++) {
            IDecisionVariable var = clusters.getNestedElement(c);
            var = RepositoryConnector.dereference(var, configuration);
            if (var instanceof CompoundVariable) {                
                CompoundVariable cVar = (CompoundVariable) var;
                String name = RepositoryConnector.getStringValue(cVar.getNestedVariable("name"));
                String host = RepositoryConnector.getStringValue(cVar.getNestedVariable("host"));
                Integer port = RepositoryConnector.getIntegerValue(cVar.getNestedVariable("monitoringPort"));
                if (null != name && null != host && null != port) {
                    if (!filter.contains(name) && !filter.contains(host)) {
                        Map<String, Object> aux = null;
                        aux = addAux(AUX_DFES, RepositoryConnector.getIntegerValue(
                            cVar.getNestedVariable("numDFEs")), aux);
                        HardwareMonitoringInfo info = new HardwareMonitoringInfo(name, host, port, aux);
                        tmp.add(info);
                    }
                }
            }
        }
        HardwareMonitoringInfo[] result;
        if (tmp.isEmpty()) {
            result = null;
        } else {
            result = new HardwareMonitoringInfo[tmp.size()];
            tmp.toArray(result);
        }
        return result;
    }

    /**
     * Adds an auxiliary value.
     * 
     * @param key the key (use <code>AUX_*</code>)
     * @param value the value matching key
     * @param aux the aux map
     * @return <code>aux</code> or a new map if <code>aux</code> is <b>null</b>
     */
    private static Map<String, Object> addAux(String key, Object value, Map<String, Object> aux) {
        if (null != value) {
            if (null == aux) {
                aux = new HashMap<String, Object>();
            }
            aux.put(key, value);
        }
        return aux;
    }
    
}