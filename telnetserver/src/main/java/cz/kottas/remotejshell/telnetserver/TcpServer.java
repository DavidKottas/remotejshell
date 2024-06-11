package cz.kottas.remotejshell.telnetserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer extends Thread implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);
    public final int port;
    public final InetAddress bindAddr;
    private final ServerSocket serverSocket;
    private final Shell shell = new Shell();

    private class SocketThread extends Thread implements Closeable {
        private final Socket socket;

        public SocketThread(Socket socket) {
            this.socket = socket;
            this.start();
        }

        @Override public void run() {
            try {
                InputStream input = socket.getInputStream();
                OutputStream output = socket.getOutputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                while (true) {
                    String line = reader.readLine();
                    byte[] outdata = null;
                    try {
                        output.write(shell.processCommand(line));
                         outdata = shell.processCommand(line);
                    } catch (Exception e) {
                        logger.error("Error processing command {}: {}", line, e.toString());
                    }
                    output.flush();
                    if (outdata != null) {
                        output.write(outdata);
                        output.flush();
                    }
                }
            } catch (Exception e) {
                logger.error(e.toString());
            }
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }

    public TcpServer(int port, InetAddress bindAddr) throws IOException  {
        this.port = port;
        this.bindAddr = bindAddr;
        serverSocket = new ServerSocket(port, 50, bindAddr);
        this.start();
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                logger.info("Got new connection from {}", socket.getInetAddress());
                new SocketThread(socket);
            }
        } catch (Exception e) {
            logger.error(e.toString());
        }

    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }
}
