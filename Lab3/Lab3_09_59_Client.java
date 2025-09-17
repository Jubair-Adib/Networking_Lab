import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Lab3_09_59_Client {
    public static void main(String[] args) {
        String serverIP = "192.168.4.73";
        int serverPort = 5000;

        try (Socket socket = new Socket(serverIP, serverPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            boolean authed = false;
            while (!authed) {
                System.out.print("Enter Card Number: ");
                String card = scanner.nextLine().trim();
                System.out.print("Enter PIN: ");
                String pin = scanner.nextLine().trim();
                out.println("AUTH:" + card + ":" + pin);
                String r = in.readLine();
                if (r == null) {
                    System.out.println("Connection closed.");
                    return;
                }
                if ("AUTH_OK".equals(r)) {
                    System.out.println("Login success.");
                    authed = true;
                } else {
                    System.out.println("AUTH_FAIL. Try again.");
                }
            }

            Thread readThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println("SERVER: " + serverMessage);
                        if (serverMessage.startsWith("WITHDRAW_OK:") || serverMessage.startsWith("INSUFFICIENT_FUNDS:")) {
                            String[] parts = serverMessage.split(":", 3);
                            if (parts.length >= 2) {
                                out.println("ACK:" + parts[1]);
                                System.out.println("ACK sent for TID: " + parts[1]);
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed.");
                }
            });
            readThread.start();
            int count=0;
            while (true) {
                System.out.println("\nSelect Option:");
                System.out.println("1) Check Balance");
                System.out.println("2) Withdraw Money");
                System.out.println("3) Exit");
                System.out.print("Choice: \n");
                String choice = scanner.nextLine().trim();

                if (choice.equals("1")) {
                    out.println("BALANCE_REQ");
                } else if (choice.equals("2") && count==0) {
                    System.out.print("Enter amount: ");
                    String amount = scanner.nextLine().trim();
                    if (amount.isEmpty()) continue;
                    String tid = String.valueOf((int)(Math.random() * 100) + 1);
                    out.println("WITHDRAW:" + tid + ":" + amount);
                    count++;
                } else if (choice.equals("2") && count==1) {
                    System.out.print("You can not withdraw again");
                } else if (choice.equals("3")) {
                    System.out.println("Exiting...");
                    out.println("exit");
                    break;
                } else {
                    System.out.println("Invalid choice. Try again.");
                }
            }

        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
}
