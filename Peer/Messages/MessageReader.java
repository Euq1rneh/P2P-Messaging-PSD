package Peer.Messages;
import Peer.Network.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class MessageReader implements Runnable {
    private final Socket socket;

    String RESET = "\u001B[0m";
    String YELLOW = "\u001B[33m";

    boolean running_status;

    public MessageReader(Socket socket, boolean running_status) {
        this.socket = socket;
        this.running_status = running_status;
    }

    @Override
    public void run() {
        try {
            boolean run = true;
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Packet packet;

            // Continuously read objects until the connection is closed
            while (running_status) {
                // Print out the Packet details
                System.out.println(YELLOW + "<------ Reading thread ------>"+ RESET);
                packet = (Packet) in.readObject();
                System.out.println(YELLOW + packet.sender + ": " + packet.message + RESET);
                System.out.println(YELLOW + "<------ End of reading thread ------>" + RESET);
            }
        } catch (IOException e) {
            System.out.println("Peer connection may have been closed unexpectedly");
        } catch (ClassNotFoundException e) {
            System.out.println("Error: Packet class not found - " + e.getMessage());
        }
    }
}
