/**
 * A simple plugin mechanism complying with the infrastructure layer boot/shutdown behavior. General idea:
 * 
 * <ul>
 *   <li>One or more plugins are realized in JAR file and placed into the plugin directory of the infrastructure.</li>
 *   <li>Plugins are classes within the jar and mentioned in the MANIFEST.MF as comma-separated qualified name
 *       list for the attribute {@code QM-Plugins}.</li>
 *   <li>A plugin is supposed allowed to have dependenvies to the required start/stop layers (but not more than 
 *       declared). We support two forms of plugins:
 *     <ol>
 *       <li>Simple classes not implementing {@link eu.qualimaster.plugins.IPlugin}: Start and shutdown layer name
 *           are given after the qualified class name brackets in the MANIFEST.MF, e.g., 
 *           {@code QM-Plugins: a.b.Class[MONITORING;ADAPTATION]} for class {@code a.b.Class} being a plugin to be
 *           started in the {@code MONITORING} layer and stopped in the {@code ADAPTATION} layer. Also just a single 
 *           layer may be given for the same start and stop.</li>
 *       <li>Full plugin classes implementing {@link eu.qualimaster.plugins.IPlugin}. Here only the qualified class
 *           name is required in the Manifest while the plugin itself provides the required information. Additional
 *           start/stop layers override the information from the plugin if given. The implementing class must have 
 *           a no-argument descriptor (irrespective whether visible or not).</li>
 *     </ol>
 *     Leading/trailing spaces around class names or layers are skipped and do not become part of the result.
 *   </li>
 * </ul>
 */
package eu.qualimaster.plugins;