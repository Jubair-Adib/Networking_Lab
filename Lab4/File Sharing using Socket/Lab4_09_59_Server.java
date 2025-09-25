import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Lab4_09_59_Server {
    private static final int PORT = 5000;
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private static final String FILE_FOLDER = "server_files";

    public static void main(String[] args) {
        File folder = new File(FILE_FOLDER);
        if (!folder.exists()) folder.mkdir();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[SERVER] File server started on port " + PORT);
            System.out.println("[SERVER] Waiting for clients...");
            System.out.println("[SERVER] Looking for files in folder: " + folder.getAbsolutePath());

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler ch = new ClientHandler(clientSocket, folder);
                clients.add(ch);
                ch.start();
                System.out.println("[SERVER] New client connected: "
                        + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
            }
        } catch (IOException e) {
            System.err.println("[SERVER] Error: " + e.getMessage());
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private final File folder;

        ClientHandler(Socket socket, File folder) {
            this.socket = socket;
            this.folder = folder;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), "UTF-8"));
                 BufferedWriter out = new BufferedWriter(
                         new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                 DataOutputStream dataOut = new DataOutputStream(socket.getOutputStream())) {

                while (true) {

                    File[] files = folder.listFiles();
                    if (files != null && files.length > 0) {
                        out.write("Available files:\n");
                        for (File f : files) {
                            out.write(" - " + f.getName() + "\n");
                        }
                    } else {
                        out.write("No files available on server.\n");
                    }
                    out.write("Enter the file name you want to download (or type /quit to exit):\n");
                    out.flush();


                    String fileName = in.readLine();
                    if (fileName == null || fileName.trim().equalsIgnoreCase("/quit")) {
                        System.out.println("[SERVER] Client disconnected by request.");
                        break;
                    }
                    fileName = fileName.trim();
                    File file = new File(folder, fileName);

                    // 3. Send file if exists
                    if (file.exists() && file.isFile()) {
                        out.write("FOUND\n");
                        out.flush();

                        long fileSize = file.length();
                        dataOut.writeLong(fileSize);

                        try (FileInputStream fis = new FileInputStream(file)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = fis.read(buffer)) != -1) {
                                dataOut.write(buffer, 0, bytesRead);
                            }
                            dataOut.flush();
                        }
                        System.out.println("[SERVER] File sent: " + fileName + " (" + file.length() + " bytes)");
                    } else {
                        out.write("NOT_FOUND\n");
                        out.flush();
                        System.out.println("[SERVER] File not found: " + fileName);
                    }
                }

            } catch (IOException e) {
                System.err.println("[SERVER] Client error: " + e.getMessage());
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
                clients.remove(this);
                System.out.println("[SERVER] Client disconnected.");
            }
        }
    }
}
