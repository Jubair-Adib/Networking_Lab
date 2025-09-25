import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Lab4_09_59_client {
    private static final String SERVER_IP = "10.33.2.207"; 
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        File downloadDir = new File("downloads"); 

        // Create downloads folder if it doesn't exist
        if (!downloadDir.exists()) {
            if (downloadDir.mkdir()) {
                System.out.println("Created downloads folder at: " + downloadDir.getAbsolutePath());
            } else {
                System.err.println("Failed to create downloads folder!");
                return;
            }
        } else {
            System.out.println("Files will be downloaded to: " + downloadDir.getAbsolutePath());
        }

        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
             DataInputStream dataIn = new DataInputStream(socket.getInputStream());
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
             Scanner sc = new Scanner(System.in)
             ) {

            System.out.println("Connected to server: " + SERVER_IP + ":" + SERVER_PORT);

            while (true) {
                // Read server messages (available files + prompt)
                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                    if (line.startsWith("Enter the file name")) break;
                }

                // Ask user for file name
                System.out.print("File to download (type 'exit' to quit): ");
                String fileName = sc.nextLine().trim();

                if ("exit".equalsIgnoreCase(fileName)) {
                    System.out.println("Exiting client...");
                    break;
                }

                // Send file request
                out.write(fileName + "\n");
                out.flush();

                // Read server response
                String response = in.readLine();
                if ("FOUND".equals(response)) {
                    long fileSize = dataIn.readLong();
                    System.out.println("Downloading file (" + fileSize + " bytes)...");

                    File outputFile = new File(downloadDir, "Download_" + fileName);

                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        long totalRead = 0;
                        while (totalRead < fileSize && (bytesRead = dataIn.read(buffer, 0,
                                (int) Math.min(buffer.length, fileSize - totalRead))) != -1) {
                            fos.write(buffer, 0, bytesRead);
                            totalRead += bytesRead;
                        }
                        fos.flush();
                    }

                    System.out.println("File downloaded successfully: " + outputFile.getAbsolutePath());
                } else {
                    System.out.println("File not found on server.");
                }
            }

        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
