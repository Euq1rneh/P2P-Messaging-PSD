package peer.network;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import peer.messages.MessageLogger;

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
        return peer_client(peer_address, peer_port);
    }

    /**
     * Sends a packet to a peer with the specified socket
     * @param packet the packet to send
     * @return 0 if the sent packet was received -1 otherwise
     */
    public static int sendPacket(Packet packet, ObjectInputStream in, ObjectOutputStream out) {
        try {
            out.writeObject(packet);
            // Flush the stream to make sure the data is sent
            //out.flush();
            System.out.println("Sent packet");

            //program can be blocked here if an ack packet is never received
            Packet ack = (Packet) in.readObject();

            if(ack == null){
                System.out.println("Did not receive ACK packet");
                return -1;
            }
            System.out.println("Received ACK packet");
            MessageLogger.write_message_log(packet.get_sender() + ": " +packet.get_data(), ack.get_sender() + ".conversation");
            return 0;
        } catch (IOException e) {
            System.out.println("Error sending packet: " + e.getMessage());
            return -1;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}