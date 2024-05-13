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
package eu.qualimaster.coordination;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple artifact registry, in particular for testing.
 * 
 * @author Holger Eichelberger
 */
public class ArtifactRegistry {

    private static final Map<String, URL> ARTIFACTS = new HashMap<String, URL>();

    /**
     * Defines a test artifact, i.e., a mapping between a (Maven) artifact specification
     * and a URL where the artifact is located. <b>Use for testing only!</b>
     * 
     * @param artifactSpec the artifact specification
     * @param url the URL (may be <b>null</b> to disable this mechanism for the given <code>artifactSpec</code>)
     */
    public static void defineArtifact(String artifactSpec, URL url) {
        if (null != artifactSpec) {
            ARTIFACTS.put(artifactSpec, url);
        }
    }
    
    /**
     * Returns the URL for a registered artifact.
     * 
     * @param artifactSpec the artifact specification
     * @return the URL or <b>null</b> if none is registered
     */
    public static URL getArtifactURL(String artifactSpec) {
        return null == artifactSpec ? null : ARTIFACTS.get(artifactSpec);
    }
    
    /**
     * Undefines an artifact.
     * 
     * @param artifactSpec the artifact specification
     */
    public static void undefineArtifact(String artifactSpec) {
        if (null != artifactSpec) {
            ARTIFACTS.remove(artifactSpec);
        }
    }

}
