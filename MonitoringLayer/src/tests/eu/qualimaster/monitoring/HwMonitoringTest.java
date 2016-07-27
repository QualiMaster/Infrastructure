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
package tests.eu.qualimaster.monitoring;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import eu.qualimaster.monitoring.hardware.HardwareConfigurationAccess;
import eu.qualimaster.monitoring.hardware.MaxelerDfeMonitor;
import eu.qualimaster.monitoring.hardware.HardwareConfigurationAccess.HardwareMonitoringInfo;
import eu.qualimaster.monitoring.systemState.PlatformSystemPart;
import eu.qualimaster.monitoring.systemState.SystemState;
import eu.qualimaster.observables.ResourceUsage;

/**
 * Implements a hardware monitoring test. However, the real hardware server may be switched off
 * so that a plain test against the hardware would not work. Thus, we implement a fake server
 * as part of this test behaving akin to the real one and test the integration.
 * 
 * If you want to test the real thing, please start the {@link MaxelerDfeMonitor} as a standalone 
 * program against the real server.
 * 
 * Actually, we do not test the event sent by {@link MaxelerDfeMonitor}. Future work as needed.
 * 
 * @author Holger Eichelberger
 */
public class HwMonitoringTest {

    private static final int PORT = 9999;
    private static final int DFES_AVAIL = 4;
    private static final int DFES_FREE = 3;
    
    /**
     * Implements a fake server.
     * 
     * @author Holger Eichelberger
     */
    private static class FakeServer implements Runnable {
        
        private ServerSocket serverSocket;
        private boolean cont = true;
        private List<FakeHandler> handlers = new ArrayList<FakeHandler>();

        /**
         * Creates the fake server.
         * 
         * @throws IOException in case of I/O problems
         */
        private FakeServer() throws IOException {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server socket created on " + PORT);            
        }
        
        /**
         * Starts the fake server.
         */
        public void start() {
            new Thread(this).start();
            System.out.println("Server thread started " + PORT);
        }
        
        /**
         * Stops the fake server.
         * 
         * @throws IOException in case of I/O problems
         */
        public void stop() throws IOException {
            System.out.println("Stopping server");
            cont = false;
            for (FakeHandler handler : handlers) {
                handler.stop();
            }
            handlers.clear();
            serverSocket.close();
            System.out.println("Stopped server");
        }

        /**
         * Executes the server thread, i.e., accepts connections.
         */
        public void run() {
            while (cont) {
                try {
                    Socket sock = serverSocket.accept();
                    System.out.println("Socket connection accepted " + PORT);
                    FakeHandler handler = new FakeHandler(sock);
                    handlers.add(handler);
                    System.out.println("Socket connection handler started");
                    new Thread(handler).start();
                } catch (SocketException e) {
                    try {
                        stop();
                    } catch (IOException e1) {
                        // ignore, stopping anyway
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }
    
    /**
     * Implements a fake socket handler receiving monitoring requests and replying on them.
     * 
     * @author Holger Eichelberger
     */
    private static class FakeHandler implements Runnable {
        
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private boolean cont = true;

        /**
         * Creates a fake handler.
         * 
         * @param socket the socket connection
         * @throws IOException in case of I/O problems
         */
        private FakeHandler(Socket socket) throws IOException {
            this.socket = socket;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }
        
        /**
         * Stops the fake handler.
         * 
         * @throws IOException in case of I/O problems
         */
        public synchronized void stop() throws IOException {
            if (null != socket) {
                System.out.println("Stopping handler");
                cont = false;
                in.close();
                in = null;
                out.close();
                out = null;
                socket.close();
                socket = null;
                System.out.println("Stopped handler");
            }
        }

        /**
         * Implements / fakes the monitoring protocol.
         */
        public void run() {
            while (cont) {
                try {
                    String req = in.readLine();
                    if (null != req) {
                        System.out.println("Request read \"" + req + "\"");
                        out.println(DFES_FREE);
                        System.out.println("Answer sent");
                    }
                } catch (SocketException e) {
                    try {
                        stop();
                    } catch (IOException e1) {
                        // ignore, stopping anyway
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }
    
    /**
     * Tests the HW monitoring integration.
     * 
     * @throws IOException in case of I/O problems (shall not occur)
     */
    @Test(timeout = 5000)
    public void testHwMonitoring() throws IOException {
        FakeServer server = new FakeServer();
        server.start();
        
        SystemState state = new SystemState();
        Map<String, Object> aux = new HashMap<String, Object>();
        aux.put(HardwareConfigurationAccess.AUX_DFES, DFES_AVAIL);
        HardwareMonitoringInfo info = new HardwareMonitoringInfo("local", "127.0.0.1", PORT, aux);
        MaxelerDfeMonitor monitor = new MaxelerDfeMonitor(info, state);
        monitor.run();
        PlatformSystemPart platform = state.getPlatform();
        int availDfes = platform.getObservedValueInt(ResourceUsage.AVAILABLE_DFES);
        System.out.println("Available DFEs: " + availDfes);
        Assert.assertEquals(DFES_AVAIL, availDfes);
        int usedDfes = platform.getObservedValueInt(ResourceUsage.USED_DFES);
        System.out.println("Used DFEs: " + usedDfes);
        Assert.assertEquals(DFES_AVAIL - DFES_FREE, usedDfes);
        monitor.stop();

        server.stop();
    }
    
}
