import java.io.*;
import java.net.*;
import java.util.*;

public class Lab3_09_59_server {
    private static final int PORT = 5000;
    private static final List<String> cardNumbers = new ArrayList<>();
    private static final List<String> pins = new ArrayList<>();
    private static final List<Integer> balances = new ArrayList<>();
    private static final List<String> txIds = new ArrayList<>();
    private static final List<String> txResponses = new ArrayList<>();

    public static void main(String[] args) {
        addAccount("12345678", "1234", 1000);
        addAccount("56781234", "5678", 1500);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Bank server started successfully");
            System.out.println("Listening on port: " + PORT);
            System.out.println("Waiting for clients");


            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("\nNew Client connected: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        }
    }

    private static void addAccount(String card, String pin, int balance) {
        cardNumbers.add(card);
        pins.add(pin);
        balances.add(balance);
    }

    private static int getAccountIndex(String card) {
        return cardNumbers.indexOf(card);
    }

    private static boolean isAuthenticated(String card, String pin) {
        int idx = getAccountIndex(card);
        return idx != -1 && pins.get(idx).equals(pin);
    }

    private static int getBalance(String card) {
        int idx = getAccountIndex(card);
        return idx != -1 ? balances.get(idx) : 0;
    }

    private static void updateBalance(String card, int newBalance) {
        int idx = getAccountIndex(card);
        if (idx != -1) balances.set(idx, newBalance);
    }

    private static String getTransactionResponse(String tid) {
        int idx = txIds.indexOf(tid);
        return idx != -1 ? txResponses.get(idx) : null;
    }

    private static void saveTransaction(String tid, String response) {
        txIds.add(tid);
        txResponses.add(response);
    }

    static class ClientHandler extends Thread {
        private final Socket socket;
        private BufferedReader in;
        private BufferedWriter out;
        private String clientCard = null;
        private String pendingTid = null;
        private String pendingResp = null;
        private int pendingAttempts = 0;

        ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                this.socket.setSoTimeout(1000);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            } catch (IOException e) {
                System.err.println("Error creating I/O streams");
            }
        }

        public void run() {
            try {
                for (;;) {
                    String line = null;
                    try { line = in.readLine(); } catch (SocketTimeoutException e) { line = null; }
                    if (line == null) {
                        if (pendingTid != null && pendingAttempts < 5) {
                            writeLine(pendingResp);
                            pendingAttempts++;
                        }
                        continue;
                    }

                    if (line.startsWith("AUTH:")) {
                        String[] p = line.split(":", 3);
                        if (p.length == 3 && isAuthenticated(p[1], p[2])) {
                            clientCard = p[1];
                            System.out.println("[AUTH] Card " + clientCard + " authenticated.");
                            writeLine("AUTH_OK");
                        } else {
                            System.out.println("[AUTH] Failed authentication attempt.");
                            writeLine("AUTH_FAIL");
                        }

                    } else if (line.equals("BALANCE_REQ")) {
                        if (clientCard == null) { writeLine("AUTH_REQUIRED"); continue; }
                        int bal = getBalance(clientCard);
                        System.out.println("[BALANCE] Card " + clientCard + " requested balance: " + bal);
                        writeLine("BALANCE_RES:" + bal);

                    } else if (line.startsWith("WITHDRAW:")) {
                        if (clientCard == null) { writeLine("AUTH_REQUIRED"); continue; }
                        String[] p = line.split(":", 3);
                        if (p.length < 3) { writeLine("ERROR"); continue; }

                        String tid = p[1];
                        String resp = getTransactionResponse(tid);

                        if (resp == null) {
                            int amt;
                            try { amt = Integer.parseInt(p[2]); } catch (Exception ex) { writeLine("ERROR"); continue; }

                            synchronized (balances) {
                                int bal = getBalance(clientCard);
                                if (bal >= amt) {
                                    updateBalance(clientCard, bal - amt);
                                    resp = "WITHDRAW_OK:" + tid + ":" + amt;
                                    System.out.println("[WITHDRAW] Card " + clientCard + " withdrew " + amt + ". Remaining balance: " + getBalance(clientCard));
                                } else {
                                    resp = "INSUFFICIENT_FUNDS:" + tid;
                                    System.out.println("[WITHDRAW] Card " + clientCard + " failed withdrawal due to insufficient funds.");
                                }
                                saveTransaction(tid, resp);
                            }
                        }
                        pendingTid = tid;
                        pendingResp = resp;
                        pendingAttempts = 0;
                        writeLine(resp);

                    } else if (line.startsWith("ACK:")) {
                        String tid = line.substring(4);
                        if (pendingTid != null && pendingTid.equals(tid)) {
                            System.out.println("[ACK] Transaction " + tid + " acknowledged by client.");
                            pendingTid = null;
                            pendingResp = null;
                            pendingAttempts = 0;
                        }
                    }
                }
            } catch (IOException ignored) {
            } finally {
                System.out.println("[-] Client disconnected: " + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void writeLine(String m) throws IOException {
            out.write(m);
            out.newLine();
            out.flush();
        }
    }
}
