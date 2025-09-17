// File: Lab3b_09_59_server.java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Lab3b_09_59_server {
    private static final int DEFAULT_PORT = 5000;

    private final int port;
    private volatile boolean running = true;
    private ServerSocket serverSocket;
    private final AtomicInteger clientIdGen = new AtomicInteger(1);
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public Lab3b_09_59_server(int port) {
        this.port = port;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("[SERVER] Chat server started on port " + port);
            System.out.println("[SERVER] Type /shutdown in this console to stop the server.");

            // console thread for graceful shutdown
            Thread console = new Thread(this::consoleLoop, "ServerConsole");
            console.setDaemon(true);
            console.start();

            while (running) {
                try {
                    Socket sock = serverSocket.accept();
                    sock.setTcpNoDelay(true);
                    int id = clientIdGen.getAndIncrement();
                    ClientHandler handler = new ClientHandler(id, sock);
                    clients.add(handler);
                    handler.start();
                    System.out.println("[SERVER] Client#" + id + " connected from " +
                            sock.getInetAddress().getHostAddress() + ":" + sock.getPort());
                } catch (SocketException se) {
                    if (running) System.err.println("[SERVER] Socket error: " + se.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Failed to start: " + e.getMessage());
        } finally {
            stopServer();
        }
    }

    private void consoleLoop() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            for (String line; running && (line = br.readLine()) != null; ) {
                if (line.trim().equalsIgnoreCase("/shutdown")) {
                    System.out.println("[SERVER] Shutdown requested.");
                    running = false;
                    closeServerSocket();
                    broadcast("[SERVER] Shutting down, goodbye.", null, true);
                    closeAllClients();
                }
            }
        } catch (IOException ignored) {}
    }

    private void closeServerSocket() {
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException ignored) {}
    }

    private void closeAllClients() {
        for (ClientHandler ch : clients) ch.safeClose();
    }

    private void stopServer() {
        running = false;
        closeServerSocket();
        closeAllClients();
        System.out.println("[SERVER] Stopped.");
    }

    /** Broadcast a message to all clients. If includeSender=false, skip the 'from' client. */
    private void broadcast(String msg, ClientHandler from, boolean includeSender) {
        for (ClientHandler ch : clients) {
            if (!includeSender && ch == from) continue;
            ch.safeSend(msg);
        }
    }

    private class ClientHandler extends Thread {
        private final int clientId;
        private final Socket socket;
        private BufferedReader in;
        private BufferedWriter out;
        private volatile boolean alive = true;
        private String displayName;

        ClientHandler(int clientId, Socket socket) {
            super("ClientHandler-" + clientId);
            this.clientId = clientId;
            this.socket = socket;
            this.displayName = "Client#" + clientId;
        }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

                send("WELCOME " + displayName + "!");
                send("Tips: Use '||' to send multiple sentences in one line.");
                send("Commands: /name <newname>  |  /quit");
                broadcast("[SERVER] " + displayName + " joined the chat.", this, false);

                String line;
                while (alive && (line = in.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) continue;

                    // client termination
                    if (trimmed.equalsIgnoreCase("/quit") || trimmed.equalsIgnoreCase("bye")) {
                        send("BYE " + displayName + ".");
                        break;
                    }

                    // rename
                    if (trimmed.toLowerCase().startsWith("/name ")) {
                        String newName = trimmed.substring(6).trim();
                        if (!newName.isEmpty()) {
                            String old = displayName;
                            displayName = newName;
                            send("[SERVER] Your name is now: " + displayName);
                            broadcast("[SERVER] " + old + " is now known as " + displayName, this, false);
                        } else {
                            send("[SERVER] Usage: /name <newname>");
                        }
                        continue;
                    }

                    // handle multiple sentences in one send
                    String[] parts = trimmed.split("\\|\\|");
                    for (String part : parts) {
                        String msg = part.trim();
                        if (!msg.isEmpty()) {
                            String formatted = "[" + displayName + "]: " + msg;
                            // show back to sender and others
                            broadcast(formatted, null, true);
                        }
                    }
                }
            } catch (IOException e) {
                // client likely disconnected abruptly
            } finally {
                cleanup();
            }
        }

        private void send(String m) throws IOException {
            out.write(m);
            out.newLine();
            out.flush();
        }

        void safeSend(String m) {
            try { if (out != null) send(m); } catch (IOException ignored) {}
        }

        void safeClose() {
            alive = false;
            try { socket.close(); } catch (IOException ignored) {}
        }

        private void cleanup() {
            alive = false;
            clients.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
            broadcast("[SERVER] " + displayName + " left the chat.", this, false);
            System.out.println("[SERVER] " + displayName + " disconnected.");
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length >= 1) {
            try { port = Integer.parseInt(args[0]); } catch (NumberFormatException ignored) {}
        }
        new Lab3b_09_59_server(port).start();
    }
}

