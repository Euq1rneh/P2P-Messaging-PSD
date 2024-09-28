package Peer.Network;

import java.io.Closeable;
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

    /**
     * Creates a socket connecting to another peer
     * @param address the ip address
     * @param port the port
     * @return the socket connecting to the peer or null if there was an error
     */
    private static Socket peer_client(String address, int port){
        try {
            return new Socket(address, port);
        } catch (IOException e) {
            System.out.println("The peer you're trying to connect to is not online");
            return null;
        }
    }

    /**
     * Closes the socket passed as an argument
     * @param socket the socket to close
     */
    public static void close_socket(Closeable socket){
        if(socket == null){
            return;
        }

        try {
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tries to establish a connection to a peer
     * @param peer_address the ip address of the peer
     * @param peer_port the port of the peer
     * @return the socket that allows communication with the peer or null if there was an error
     */
    public static Socket try_connect_to_peer(String peer_address, int peer_port){
        System.out.printf("Trying to connect to %s:%d\n", peer_address, peer_port);
        return peer_client(peer_address, peer_port);
    }

    /**
     * Sends a packet to a peer with the specified socket
     * @param packet the packet to send
     * @param socket the socket used for packet transmission
     */
    public static void sendPacket(Packet packet, Socket socket) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(packet);
            // Flush the stream to make sure the data is sent
            out.flush();

            System.out.println("Sent packet");
        } catch (IOException e) {
            System.out.println("Error sending packet: " + e.getMessage());
        }
    }
}
