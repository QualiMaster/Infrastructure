package eu.qualimaster.monitoring.spassMeter;

import java.util.Properties;

import de.uni_hildesheim.sse.monitoring.runtime.plugins.IPluginParameter;
import de.uni_hildesheim.sse.monitoring.runtime.plugins.Plugin;
import de.uni_hildesheim.sse.monitoring.runtime.plugins.PluginRegistry;
import eu.qualimaster.Configuration;

/**
 * The QualiMaster-SPASS-integration plugin for SPASS-meter. For proper functionality,
 * SPASS-meter needs to be configured with the event bus parameters of the QM platform. See
 * {@link eu.qualimaster.Configuration#HOST_EVENT} and {@link eu.qualimaster.Configuration#PORT_EVENT}. 
 * This class assumes that monitoring happens with <code>instanceIdentifierKind=IDENTITIY_HASHCODE</code> 
 * enabled. To meet SPASS-meter installation guidelines and the QualiMaster deployment structure, there 
 * shall not be any dependency from infrastructure code to the SPASS-meter integration and only dependencies
 * from the SPASSS-meter integration to QualiMaster.Events as this part of the infrastructure is available
 * to all processing nodes.
 * 
 * @author Holger Eichelberger
 */
public class QmPlugin implements Plugin {

    /**
     * Public constructor according to plugin conventions.
     */
    public QmPlugin() {
    }
    
    @Override
    public void start(IPluginParameter parameter) {
        Properties props = new Properties();
        String tmp = parameter.get(Configuration.HOST_EVENT);
        if (null != tmp) {
            props.put(Configuration.HOST_EVENT, tmp);
        }
        tmp = parameter.get(Configuration.PORT_EVENT);
        if (null != tmp) {
            try {
                props.put(Configuration.PORT_EVENT, Integer.valueOf(tmp));
            } catch (NumberFormatException e) {
            }
        }
        if (props.size() > 0) {
            Configuration.configure(props, false);
        }
        SpassListener listener = new SpassListener();
        PluginRegistry.attachMonitoringGroupCreationListener(listener);
        PluginRegistry.attachMonitoringGroupBurstChangeListener(listener);
    }

    @Override
    public void stop() {
    }

}
