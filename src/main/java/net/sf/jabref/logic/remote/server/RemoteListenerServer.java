package net.sf.jabref.logic.remote.server;

import net.sf.jabref.logic.remote.shared.Protocol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class RemoteListenerServer implements Runnable {

    private static final int BACKLOG = 1;
    private static final int ONE_SECOND_TIMEOUT = 1000;

    private static final Log LOGGER = LogFactory.getLog(RemoteListenerServer.class);

    private final MessageHandler messageHandler;
    private final ServerSocket serverSocket;

    public RemoteListenerServer(MessageHandler messageHandler, int port) throws IOException {
        this.serverSocket = new ServerSocket(port, BACKLOG, InetAddress.getByName("localhost"));
        this.messageHandler = messageHandler;
    }

    public void run() {
        try {
            while (!Thread.interrupted()) {
                try {
                    Socket socket = serverSocket.accept();
                    socket.setSoTimeout(ONE_SECOND_TIMEOUT);

                    Protocol protocol = new Protocol(socket);
                    try {
                        protocol.sendMessage(Protocol.IDENTIFIER);
                        String message = protocol.receiveMessage();
                        if (message.isEmpty()) {
                            continue;
                        }
                        messageHandler.handleMessage(message);
                    } finally {
                        protocol.close();
                    }

                } catch (SocketException ex) {
                    return;
                } catch (IOException e) {
                    LOGGER.warn("RemoteListenerServer crashed", e);
                }
            }
        } finally {
            closeServerSocket();
        }
    }

    public void closeServerSocket() {
        try {
            serverSocket.close();
        } catch (IOException ignored) {

        }
    }

}
