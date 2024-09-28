package Peer.Messages;
import Peer.Network.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

public class MessageReader implements Runnable {
    private final Socket socket;

    public MessageReader(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Use ObjectInputStream to read serialized Packet objects
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Packet packet;

            // Continuously read objects until the connection is closed
            // will throw IOException after reading one packet
            // possibly because both peers close the socket
            while ((packet = (Packet) in.readObject()) != null) {
                // Print out the Packet details
                System.out.println(packet.sender + ": " + packet.message);
            }
        } catch (IOException e) {
            System.out.println("Error reading from peer: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("Error: Packet class not found - " + e.getMessage());
        } finally {
            try {
                socket.close(); // NEED TO BE CAREFUL BECAUSE BOTH SENDER AND READER CLOSE THE SOCKET WHICH GENERATES EXCEPTION
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}
