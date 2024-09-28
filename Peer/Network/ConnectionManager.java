package Peer.Network;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionManager {

    public static ServerSocket peer_server(int port){
        try {
           return new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("<-----Error creating peer input socket----->");
            return null;
        }
    }

    private static Socket peer_client(String address, int port){
        try {
            return new Socket(address, port);
        } catch (IOException e) {
            System.out.println("The peer you're trying to connect to is not online");
            return null;
        }
    }

    public static void close_socket(Socket socket){
        if(socket.isClosed()){
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Socket try_connect_to_peer(String peer_address, int peer_port){
        System.out.printf("Trying to connect to %s:%d\n", peer_address, peer_port);
        return peer_client(peer_address, peer_port);
    }


    public static void sendPacket(Packet packet, Socket socket) {
        try {
            // Create an ObjectOutputStream to send serialized Packet objects
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            // Write the Packet object to the output stream
            out.writeObject(packet);
            // Flush the stream to make sure the data is sent
            out.flush();

            System.out.println("Sent packet: " + packet.message);
        } catch (IOException e) {
            System.out.println("Error sending packet: " + e.getMessage());
        } finally {
            try {
                // Close the socket once the packet is sent
                socket.close(); // NEED TO BE CAREFUL BECAUSE BOTH SENDER AND READER CLOSE THE SOCKET WHICH GENERATES EXCEPTION
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}
