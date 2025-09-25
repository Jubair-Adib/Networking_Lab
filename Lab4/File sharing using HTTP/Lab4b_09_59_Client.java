import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Lab4b_09_59_Client {
    
    private static final String SERVER = "http://localhost:8080";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        File downloadDir = new File("downloads");
        if (!downloadDir.exists()) downloadDir.mkdir();

        while (true) {
            System.out.println("\n=== HTTP FILE CLIENT ===");
            System.out.println("1. Upload file");
            System.out.println("2. Download file");
            System.out.println("3. Exit");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();

            if ("1".equals(choice)) {
                System.out.print("Enter local file path to upload: ");
                String path = sc.nextLine().trim();
                uploadFile(path);
            } 
            else if ("2".equals(choice)) {
                System.out.print("Enter filename to download (exact name on server): ");
                String name = sc.nextLine().trim();
                File saveTo = new File(downloadDir, "download_" + name);
                downloadFile(name, saveTo);
            } 
            else if ("3".equals(choice)) {
                break;
            } 
            else {
                System.out.println("Invalid choice.");
            }
        }
        sc.close();
        System.out.println("Client exited.");
    }

    private static void uploadFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            System.out.println("File not found: " + filePath);
            return;
        }

        System.out.println("[CLIENT] Connecting to server to upload: " + file.getName());
        try {
            
            String encodedName = URLEncoder.encode(file.getName(), "UTF-8");
            URL url = new URL(SERVER + "/upload?filename=" + encodedName);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/octet-stream");

            try (OutputStream os = con.getOutputStream();
                 FileInputStream fis = new FileInputStream(file)) {
                byte[] buf = new byte[4096];
                int len;
                long sent = 0;
                while ((len = fis.read(buf)) != -1) {
                    os.write(buf, 0, len);
                    sent += len;
                }
                os.flush();
                System.out.println("[CLIENT] Sent " + sent + " bytes to server");
            }

            int code = con.getResponseCode();
            System.out.println("[CLIENT] Server response code: " + code);
            try (InputStream is = (code >= 200 && code < 400) ? con.getInputStream() : con.getErrorStream()) {
                if (is != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("[CLIENT] Server message: " + line);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("[CLIENT] Upload error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void downloadFile(String filename, File saveTo) {
        System.out.println("[CLIENT] Connecting to server to download: " + filename);
        try {
            URL url = new URL(SERVER + "/download?filename=" + URLEncoder.encode(filename, "UTF-8"));
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int code = con.getResponseCode();
            System.out.println("[CLIENT] Server response code: " + code);

            if (code == 200) {
                try (InputStream in = con.getInputStream();
                     FileOutputStream fos = new FileOutputStream(saveTo)) {
                    byte[] buf = new byte[4096];
                    int len;
                    long received = 0;
                    while ((len = in.read(buf)) != -1) {
                        fos.write(buf, 0, len);
                        received += len;
                    }
                    System.out.println("[CLIENT] File downloaded successfully: " + saveTo.getAbsolutePath());
                    System.out.println("[CLIENT] Total bytes received: " + received);
                }
            } else {
                try (InputStream es = con.getErrorStream()) {
                    if (es != null) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(es));
                        String line;
                        while ((line = br.readLine()) != null) {
                            System.out.println("[CLIENT] Server message: " + line);
                        }
                    }
                }
                if (code == 404) {
                    System.out.println("[CLIENT] File not found on server.");
                } else {
                    System.out.println("[CLIENT] Download failed.");
                }
            }
        } catch (IOException e) {
            System.out.println("[CLIENT] Download error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
