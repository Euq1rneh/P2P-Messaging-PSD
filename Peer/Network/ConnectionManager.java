package Peer.Network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionManager {

    public static ServerSocket peer_server(int port){
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Peer server running on port " + port + "...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Peer connected to server with address: " + clientSocket.getInetAddress());
                //call method for message handling

                return serverSocket;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void close_peer_server(ServerSocket socket){

        if(socket.isClosed()){
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("The threads that were accepting request were unexpectedly close");
            throw new RuntimeException(e);
        }

    }

    public static int try_connect_to_peer(int peer_address){
        System.out.println("Trying to connect to " + peer_address + "...");
        return -1;
    }
}
