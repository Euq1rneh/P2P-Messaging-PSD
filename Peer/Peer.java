package Peer;

import Peer.Network.ConnectionManager;

import java.net.ServerSocket;

public class Peer {

    private String name;
    private int port;

    private ServerSocket peer_server;

    public Peer(String name, int port){
        this.name = name;
        this.port = port;
    }

    public int send_message(String msg, int conversation_id){
        return -1;
    }

    public int open_conversation(int conversation_id){
        return -1;
    }

    public void list_conversations(){

    }

    public void start(){
        // Start peer server for accepting messaging requests
        peer_server = ConnectionManager.peer_server(port);

        if(peer_server == null){
            System.out.println("<------Error starting peer------>");
            return;
        }
    }
}
