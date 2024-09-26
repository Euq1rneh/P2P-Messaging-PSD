package Peer.Network;

import java.io.IOException;
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
            throw new RuntimeException(e);
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

    public static int try_connect_to_peer(String peer_address, int peer_port){
        System.out.printf("Trying to connect to %s:%d\n", peer_address, peer_port);
        Socket s = peer_client(peer_address, peer_port);

        close_socket(s);
        return -1;
    }
}
