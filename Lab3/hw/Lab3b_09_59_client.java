// File: Lab3b_09_59_client.java
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Lab3b_09_59_client {
    private static final String DEFAULT_HOST = "192.168.1.108"; // change to your server IP
    private static final int DEFAULT_PORT = 5000;

    // static counter for anonymous users
    private static int anonymousCounter = 1;

    public static void main(String[] args) {
        String host = (args.length >= 1) ? args[0] : DEFAULT_HOST;
        int port = (args.length >= 2) ? parseIntOrDefault(args[1], DEFAULT_PORT) : DEFAULT_PORT;

        System.out.println("[CLIENT] Connecting to " + host + ":" + port + " ...");

        try (Socket socket = new Socket(host, port);
             BufferedReader in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
             Scanner sc = new Scanner(System.in)) {

            socket.setTcpNoDelay(true);

            // === Ask for client name first ===
            System.out.print("Enter your name: ");
            String clientName = sc.nextLine().trim();

            if (clientName.isEmpty()) {
                clientName = "Anonymous" + anonymousCounter;
                anonymousCounter++;
            }

            System.out.println("Welcome " + clientName + " to the joined chat...");

            // Reader thread for incoming messages
            Thread reader = new Thread(() -> {
                try {
                    String s;
                    while ((s = in.readLine()) != null) {
                        System.out.println(s);
                    }
                } catch (IOException e) {
                    System.out.println("[CLIENT] Disconnected from server.");
                }
            }, "ServerReader");
            reader.setDaemon(true);
            reader.start();

            System.out.println("[CLIENT] You can now type messages.");
            System.out.println("[CLIENT] Use /quit to exit.");

            // Writer loop
            while (true) {
                if (!sc.hasNextLine()) break;
                String line = sc.nextLine();
                if (line == null) break;

                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                if (trimmed.equalsIgnoreCase("/quit")) {
                    writeLine(out, "/quit");
                    break;
                }

                // Prepend name before sending
                String message = clientName + ": " + trimmed;
                writeLine(out, message);
            }
        } catch (IOException e) {
            System.err.println("[CLIENT] Error: " + e.getMessage());
        }

        System.out.println("[CLIENT] Bye.");
    }

    private static void writeLine(BufferedWriter out, String m) {
        try {
            out.write(m);
            out.newLine();
            out.flush();
        } catch (IOException e) {
            System.err.println("[CLIENT] Failed to send: " + e.getMessage());
        }
    }

    private static int parseIntOrDefault(String s, int def) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }
}

