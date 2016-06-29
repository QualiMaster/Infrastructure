package eu.qualimaster.base.algorithm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;


/**
 * Define the transmitter Specifying the protocol for hareware communication.
 * @author Apostolos Nydriotis, Cui Qin, Gregory Chrysos
 *
 */
public class HardwareTransmitter {

    private String ip;
    private int port;

    private Socket sock = null;
    private PrintWriter out = null;
    private OutputStream byteOut = null;

    private InputStream in = null;

    /**
     * Transmitter constructor without ip and port specification.
     * @throws IOException io exception
     */
    public HardwareTransmitter() throws IOException {
        connect();
    }

    /**
     * Transmitter constructor connecting to the specific host.
     * @param ip the ip address to be connected
     * @param port the port to be connected
     * @throws IOException io exception
     */
    public HardwareTransmitter(String ip, int port) throws IOException {
        this.ip = ip;
        this.port = port;
        connect();
    }
    
    /**
     * Connect to the specific host.
     * @throws IOException io exception
     */
    public void connect() throws IOException {
        System.out.println("Connecting to " + ip + " : " + port);
        sock = new Socket(ip, port);
        byteOut = sock.getOutputStream();
        out = new PrintWriter(byteOut, true);
        in = sock.getInputStream();
    }
    
    /**
     * Returns the socket.
     * @return the socket
     */
    public Socket getSocket() {
        return sock;
    }
    /**
     * Receive message from Server.
     * 
     * @return the received data
     */
    public byte[] recvMsg() {
        byte[] temp = null;
        try {
            byte[] len = new byte[1024];
            int count = in.read(len);

            temp = new byte[count];
            for (int i = 0; i < count; i++) {
                temp[i] = len[i];
            }
        } catch (IOException e) {
            System.out.println("recvMsg() occur exception!" + e.toString());
        }
        return temp;
    }
    /**
     * Receives data from Server.
     * @param msg a byte array for storing new data
     * @param pointer the start offset in array b at which the data is written
     * @param maxDataSize the maximum number of bytes to read
     * @return the bytes read into the buffer
     * @throws IOException IO exception
     */
    public byte[] receiveData(byte[] msg, int pointer, int maxDataSize) throws IOException {
        while (pointer < maxDataSize) {
            if (sock.getInputStream().read(msg, pointer, 1) >= 0) {
                pointer++;
            }
        }
        return msg;
    }
    /**
     * Receives the batch data from Hardware.
     * @return a batch of data
     * @throws IOException IO Exception
     */
    public byte[] receiveData() throws IOException {
        byte[] msg = new byte[1024];
        byte[] temp = new byte[1];

        int counter = 0;
        while (true) {
            sock.getInputStream().read(temp, 0, 1);
            if (temp[0] == '\0') {
                break;
            } else {
                msg[counter] = temp[0];
                counter++;
            }
        }
        return (msg);
    }
    
    /**
     * Receives data from Server.
     * @param msg a byte array for storing new data
     * @param pointer the start offset in array b at which the data is written
     * @return the bytes read into the buffer
     * @throws IOException IO exception
     */
    public byte[] receiveData(byte[] msg, int pointer) throws IOException {
        int counter = pointer;
        byte[] result;
        String str;
        if (counter < 1) {
            while (true) {
                if (sock.getInputStream().read(msg, 0, 1) > 0) {
                    counter = 1;
                    break;
                }
            }
        }
        str = new String(msg, 0, 1);
        if (str.equalsIgnoreCase("f")) {
            result = msg;
        } else {
            while (true) {
                if (sock.getInputStream().read(msg, counter, 1) > 0) {
                    str = new String(msg, counter, 1);
                    if (str.equalsIgnoreCase("d") || str.equalsIgnoreCase("f")) {
                        result = msg;
                        break;
                    }
                    counter++;
                }
            }
        }
        return result;
    }
    
    /**
     * Receives the batch data from hardware.
     * @param msg a byte array for storing new data
     * @param batchSize the batch size to be read once
     * @throws IOException IO Exception
     * @return data bytes
     */
    public byte[] receiveBatchData(byte[] msg, int batchSize) throws IOException {
        sock.getInputStream().read(msg , 0, batchSize);
        return msg;
    }
    /**
     * Sends string data to the hardware.
     * @param data the string data to be sent
     * @throws IOException io exception
     */
    void send(String data) throws IOException {
        out.println(data);
    }

    /**
     * Sends a close messgae.
     */
    public void sendCloseMessage() {
        sendData("ca".getBytes());
    }
    /**
     * Sends a flush messgae.
     */
    public void sendFlushMessage() {
        sendData("cb".getBytes());
    }
    /**
     * Sends the separator.
     * @param separator the separator string
     */
    public void sendSeparator(String separator) {
        sendData(separator.getBytes());
    }
    
    /**
     * Closes the io operations.
     * @throws IOException io exception
     */
    public void close() throws IOException {
        if (out != null) {
            out.close(); //also close the byteOut
        }
        if (in != null) {
            in.close();
        }
        if (sock != null) {
            sock.close();
        }
    }
    
    // checkstyle: stop exception type check

    /**
     * Sends the byte array to the hardware. 
     * @param byteArray the byte array to be sent
     */
    public void sendData(byte[] byteArray) {
        try {
            if (sock == null) {
                connect();
            } 
            byteOut.write(byteArray);
        } catch (Throwable e) {
            //TODO: if the connection fails, we shall buffer the data.
            e.printStackTrace(); 
            try {
                close();
            } catch (Throwable e1) {
                e1.printStackTrace();
            }
        } 
    }

    // checkstyle: resume exception type check

}
