package Peer;

import Peer.Messages.MessageReader;
import Peer.Network.ConnectionManager;
import Peer.Network.Packet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Peer {

    private String name;
    private final int in_port;
    private final int out_port;

    private ServerSocket peer_in;
    private Socket peer_out;

    public Peer(String name, int in_port, int out_port){
        this.name = name;
        this.in_port = in_port;
        this.out_port = out_port;
    }

    public int send_message(String msg, String peer_address, int peer_port){
        if(msg == null || msg.isEmpty() || msg.isBlank()){
            System.out.println("No message was provided");
            return -1;
        }



        //should check if the ip and port match if so there is no need to disconect
        if(peer_out != null && peer_out.isConnected()){
            try {
                peer_out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        peer_out = ConnectionManager.try_connect_to_peer(peer_address, peer_port);

        if(peer_out == null){
            System.out.printf("Could not establish connection to peer (%s:%d)\n", peer_address, peer_port);
            return -1;
        }
        System.out.println("Connection successful");

        Packet p = new Packet(name, msg);

        ConnectionManager.sendPacket(p, peer_out);

        return 0;
    }

    public int open_conversation(int conversation_id){
        return -1;
    }

    public void list_conversations(){

    }

    public void start(){
        // Create a thread to handle accepting connections
        Thread connectionAcceptorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (ServerSocket serverSocket = ConnectionManager.peer_server(in_port)) {
                    peer_in = serverSocket;
                    while (!serverSocket.isClosed()) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            System.out.println("Accepted connection from peer" + clientSocket.getInetAddress().getHostAddress());

                            new Thread(new MessageReader(clientSocket)).start();
                        } catch (IOException e) {
                            System.out.println("Error accepting connection");
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    System.out.println("There was an error while trying to start peer");
                    e.printStackTrace();
                }
            }
        });

        // Start the thread that accepts connections
        connectionAcceptorThread.start();
    }
}
