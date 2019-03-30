/*
 * Copyright 2009-2019 University of Hildesheim, Software Systems Engineering
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
package tests.eu.qualimaster.testSupport;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Hosts on which certain tests cannot be executed.
 * Comma-separated host names in {@code -Dtest.exclude.hosts}.
 * 
 * @author Holger Eichelberger
 */
public class TestExcludeHosts {

    private static final Set<String> HOSTS = new HashSet<String>();

    static {
        String tmp = System.getProperty("test.exclude.hosts", "");
        StringTokenizer hosts = new StringTokenizer(tmp, ",");
        while (hosts.hasMoreTokens()) {
            HOSTS.add(hosts.nextToken());
        }
    }
    
    /**
     * Returns whether the running machine is identified as an excluded host and some tests shall not run.
     * 
     * @return <code>true</code> if the running machine is an excluded host , <code>false</code> else
     */
    public static boolean isExcludedHost() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            hostname = "localhost";
        }
        return HOSTS.contains(hostname);
    }

}
