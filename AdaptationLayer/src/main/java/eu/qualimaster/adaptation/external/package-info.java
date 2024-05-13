/**
 * A communication mechanism with the configuration tool and the external outbound server of the 
 * QualiMaster infrastructure. The implementation shall not rely on other libraries/classes rather than
 * the basic Java implementation in order to avoid library conflicts.
 * 
 * Protocol rules:
 * <ul>
 *   <li>If a {@link eu.qualimaster.adaptation.external.ClientEndpoint} is created, it connects to its given
 *       server or fails.</li>
 *   <li>{@link eu.qualimaster.adaptation.external.UsualMessage Usual messages} or 
 *       {@link eu.qualimaster.adaptation.external.DisconnectRequest} can be sent without authentication.</li>
 *   <li>The server side reacts with a {@link eu.qualimaster.adaptation.external.ExecutionResponseMessage}.</li>
 *   <li>{@link eu.qualimaster.adaptation.external.ConnectedMessage} can be used as a kind of ping.</li>
 *   <li>{@link eu.qualimaster.adaptation.external.PrivilegedMessage Privileged messages} require prior 
 *       successful {@link eu.qualimaster.adaptation.external.AuthenticateMessage authentication}. Some messages like
 *       {@link eu.qualimaster.adaptation.external.ChangeParameterRequest} or 
 *       {@link eu.qualimaster.adaptation.external.SwitchAlgorithmRequest} can act as usual or privileged message.
 *       Basically, they are creates as usual and handled then with lower priority. However, they can be 
 *       {@link Message#elevate() elevated} to a priority message.</li>
 *   <li>If an unauthorized client sends a privileged / elevated message, the server side will just reject it via
 *       an unsuccessful {@link eu.qualimaster.adaptation.external.ExecutionResponseMessage}.</li>
 *   <li>{@link eu.qualimaster.adaptation.external.HardwareAliveMessage}, 
 *       {@link eu.qualimaster.adaptation.external.MonitoringDataMessage} and 
 *       {@ink eu.qualimaster.adaptation.external.AlgorithmChangedMessage} are created by the server side on purpose 
 *       without direct request and are subject to revision. 
 *       {@link eu.qualimaster.adaptation.external.HardwareAliveMessage} and 
 *       {@link eu.qualimaster.adaptation.external.MonitoringDataMessage} are not passed to unauthenticated clients. 
 *       </li>
 *   <li>A connection is terminated by a {@link eu.qualimaster.adaptation.external.DisconnectRequest}.</li>
 * </ul>
 * 
 * 
 * Please note that for testing messages must implement hashcode and equals.
 */
package eu.qualimaster.adaptation.external;