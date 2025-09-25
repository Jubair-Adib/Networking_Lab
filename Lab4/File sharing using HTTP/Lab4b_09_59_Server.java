import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class Lab4b_09_59_Server {
    private static final int PORT = 8080;
    private static final String FILE_DIR = "server_files";

    public static void main(String[] args) throws IOException {
        File dir = new File(FILE_DIR);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                System.err.println("Failed to create directory: " + dir.getAbsolutePath());
                return;
            }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/download", new DownloadHandler());
        server.createContext("/upload", new UploadHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();

        System.out.println("=== HTTP FILE SERVER STARTED ===");
        System.out.println("Port: " + PORT);
        System.out.println("Files directory: " + dir.getAbsolutePath());
        System.out.println("Endpoints:");
        System.out.println("Download: /download?filename=<file>");
        System.out.println("Upload:   /upload?filename=<file>");
    }

    static class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String client = exchange.getRemoteAddress().toString();
            System.out.println("[SERVER] Client " + client + " requested download.");

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                System.out.println("[SERVER] Method not allowed: " + exchange.getRequestMethod());
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String query = exchange.getRequestURI().getQuery();
            String filename = null;
            if (query != null) {
                Map<String, String> params = parseQuery(query);
                if (params.containsKey("filename")) {
                    filename = URLDecoder.decode(params.get("filename"), "UTF-8");
                }
            }

            if (filename == null || filename.isEmpty()) {
                String msg = "Missing filename parameter";
                System.out.println("[SERVER] " + msg);
                byte[] resp = msg.getBytes();
                exchange.sendResponseHeaders(400, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
                exchange.close();
                return;
            }

            File file = new File(FILE_DIR, filename);
            if (!file.exists() || !file.isFile()) {
                String msg = "File Not Found: " + filename;
                System.out.println("[SERVER] " + msg);
                byte[] resp = msg.getBytes();
                exchange.sendResponseHeaders(404, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
                exchange.close();
                return;
            }

            System.out.println("[SERVER] Sending file: " + filename + " (" + file.length() + " bytes)");
            Headers headers = exchange.getResponseHeaders();
            headers.set("Content-Type", "application/octet-stream");
            headers.set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
            exchange.sendResponseHeaders(200, file.length());

            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int count;
                long sent = 0;
                while ((count = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, count);
                    sent += count;
                }
                System.out.println("[SERVER] File sent successfully: " + sent + " bytes");
            } catch (IOException e) {
                System.err.println("[SERVER] Error sending file: " + e.getMessage());
                e.printStackTrace();
            } finally {
                exchange.close();
            }
        }
    }

    static class UploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String client = exchange.getRemoteAddress().toString();
            System.out.println("[SERVER] Client " + client + " requested upload.");

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                System.out.println("[SERVER] Method not allowed: " + exchange.getRequestMethod());
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String filename = null;
            String query = exchange.getRequestURI().getQuery();
            if (query != null) {
                Map<String, String> params = parseQuery(query);
                if (params.containsKey("filename")) {
                    filename = URLDecoder.decode(params.get("filename"), "UTF-8");
                }
            }
            if (filename == null || filename.trim().isEmpty()) {
                filename = "upload_" + System.currentTimeMillis();
            }

            File outFile = new File(FILE_DIR, filename);
            long total = 0;

            try (InputStream in = exchange.getRequestBody();
                 FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buffer = new byte[4096];
                int count;
                while ((count = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, count);
                    total += count;
                }
                fos.flush();
            } catch (IOException e) {
                System.err.println("[SERVER] Error while receiving upload: " + e.getMessage());
                e.printStackTrace();
                String err = "Upload failed: " + e.getMessage();
                byte[] resp = err.getBytes();
                exchange.sendResponseHeaders(500, resp.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp);
                }
                exchange.close();
                return;
            }

            System.out.println("[SERVER] File uploaded successfully: " + filename + " (" + total + " bytes)");
            String msg = "File uploaded as: " + filename;
            byte[] response = msg.getBytes();
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            } finally {
                exchange.close();
            }
        }
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] parts = query.split("&");
        for (String part : parts) {
            int idx = part.indexOf('=');
            if (idx >= 0) {
                String key = part.substring(0, idx);
                String val = part.substring(idx + 1);
                map.put(key, val);
            } else {
                map.put(part, "");
            }
        }
        return map;
    }
}
