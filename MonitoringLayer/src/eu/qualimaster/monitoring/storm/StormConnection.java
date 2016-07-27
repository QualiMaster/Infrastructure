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
package eu.qualimaster.monitoring.storm;

import java.net.SocketException;

import org.apache.storm.curator.framework.CuratorFramework;
import org.apache.thrift7.transport.TTransportException;

import eu.qualimaster.common.signal.ThriftConnection;
import eu.qualimaster.coordination.ZkUtils;

/**
 * Storm connection, extends the thrift connection by a curator connection.
 * 
 * @author Holger Eichelberger
 */
public class StormConnection extends ThriftConnection {

    private CuratorFramework curator;

    /**
     * Creates a connection from the infrastucture configuration object.
     */
    public StormConnection() {
        super();
    }
    
    /**
     * Creates a connection with explicit nimbus host.
     * 
     * @param nimbusHost the nimbus host name
     * @param port the nimbus port number
     */
    public StormConnection(String nimbusHost, int port) {
        super(nimbusHost, port);
    }

    @Override
    public boolean isOpen() {
        return super.isOpen() && null != curator;
    }
    
    @Override
    public boolean open() {
        boolean open = super.open();
        if (null == curator) {
            curator = ZkUtils.obtainCuratorFramework();
        }
        return open && null != curator;
    }

    @Override
    public void close() {
        super.close();
        if (null != curator) {
            curator.close();
        }
    }
    
    /**
     * Returns the curator framework instance.
     * 
     * @return the framework instance
     */
    public CuratorFramework getCurator() {
        return curator;
    }
    
    /**
     * Performs a failover, e.g., when a network connection closed due to failures.
     * 
     * @param th the throwable causing the need for failover
     */
    public void failover(Throwable th) {
        if ((th instanceof TTransportException) || (th instanceof SocketException)) { 
            close(); // hope that it silently closes and a reconnect happens in the next round
        }
    }
    
}
