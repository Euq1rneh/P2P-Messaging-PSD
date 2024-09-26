import java.io.*;
import java.net.*;
import java.util.Scanner;

public class P2PMessagingApp {

    private static final int PORT = 12346; // Define the port for the server socket
    private static Socket peerSocket = null;
    private static ServerSocket serverSocket = null;
    private static boolean running = true;

    public static void main(String[] args) throws IOException {
        // Start server thread to accept connections
        new Thread(new ConnectionListener()).start();

        // Main thread to send messages and read terminal commands
        Scanner scanner = new Scanner(System.in);

        while (running) {
            System.out.println("Enter a command: ");
            String input = scanner.nextLine();

            if (input.startsWith("connect")) {
                // Example command: connect 127.0.0.1 12345
                String[] command = input.split(" ");
                if (command.length == 3) {
                    String host = command[1];
                    int port = Integer.parseInt(command[2]);
                    connectToPeer(host, port);
                } else {
                    System.out.println("Usage: connect <host> <port>");
                }
            } else if (input.startsWith("send")) {
                // Example command: send Hello World
                String message = input.substring(5); // Remove "send " prefix
                sendMessage(message);
            } else if (input.equals("exit")) {
                running = false;
                closeConnection();
                System.out.println("Exiting...");
            } else {
                System.out.println("Unknown command.");
            }
        }
    }

    // Connect to another peer
    private static void connectToPeer(String host, int port) {
        try {
            peerSocket = new Socket(host, port);
            System.out.println("Connected to peer at " + host + ":" + port);

            // Start a thread to read incoming messages from the peer
            new Thread(new PeerMessageReader(peerSocket)).start();
        } catch (IOException e) {
            System.out.println("Could not connect to peer: " + e.getMessage());
        }
    }

    // Send a message to the connected peer
    private static void sendMessage(String message) {
        if (peerSocket != null && peerSocket.isConnected()) {
            try {
                PrintWriter out = new PrintWriter(peerSocket.getOutputStream(), true);
                out.println(message);
                System.out.println("Sent: " + message);
            } catch (IOException e) {
                System.out.println("Error sending message: " + e.getMessage());
            }
        } else {
            System.out.println("No peer connected.");
        }
    }

    // Close the peer socket
    private static void closeConnection() {
        try {
            if (peerSocket != null) {
                peerSocket.close();
            }
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }

    // Runnable for listening and accepting incoming connections
    static class ConnectionListener implements Runnable {
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(PORT);
                System.out.println("Server listening on port " + PORT);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Accepted connection from " + clientSocket.getInetAddress());

                    // Start a thread to handle the new client connection
                    new Thread(new PeerMessageReader(clientSocket)).start();
                }
            } catch (IOException e) {
                System.out.println("Server error: " + e.getMessage());
            }
        }
    }

    // Runnable for reading messages from peers
    static class PeerMessageReader implements Runnable {
        private Socket socket;

        public PeerMessageReader(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received: " + message);
                }
            } catch (IOException e) {
                System.out.println("Error reading from peer: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Error closing socket: " + e.getMessage());
                }
            }
        }
    }
}

