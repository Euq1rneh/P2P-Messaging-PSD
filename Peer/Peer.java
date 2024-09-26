package Peer;

import Peer.Network.ConnectionManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Peer {

    private String name;
    private int port;

    private ServerSocket peer_in;

    public Peer(String name, int port){
        this.name = name;
        this.port = port;
    }

    public int send_message(String msg, String peer_address, int peer_port){
        if(msg.isEmpty() || msg.isBlank() || msg == null){
            System.out.println("No message was provided");
            return -1;
        }

        if(ConnectionManager.try_connect_to_peer(peer_address, peer_port) == -1){
            System.out.printf("Could not establish connection to peer (%s:%d)\n", peer_address, peer_port);
            return -1;
        }

        return -1;
    }

    public int open_conversation(int conversation_id){
        return -1;
    }

    public void list_conversations(){

    }

    public void start(){
        // Start peer server for accepting messaging requests
//        peer_in = ConnectionManager.peer_server(port);
//        if(peer_in == null){
//            System.out.println("<------Error starting peer------>");
//            return;
//        }
//        if(peer_in.isClosed()){
//            System.out.println("Peer in is closed");
//            return;
//        }else{
//            System.out.println("Peer in socket created");
//        }

        // Create a thread to handle accepting connections
        Thread connectionAcceptorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (ServerSocket serverSocket = ConnectionManager.peer_server(port)) {
                    //System.out.println("Server started on port " + port + ", waiting for connections...");

                    // Continuously accept incoming client connections
                    while (!serverSocket.isClosed()) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            System.out.println("Accepted connection from " + clientSocket.getInetAddress().getHostAddress());

                            // Optionally handle the clientSocket in another thread here if needed
                        } catch (IOException e) {
                            System.out.println("Error accepting connection");
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Error creating server socket");
                    e.printStackTrace();
                }
            }
        });

        // Start the thread that accepts connections
        connectionAcceptorThread.start();
    }
}
