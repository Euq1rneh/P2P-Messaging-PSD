package Peer;

import Peer.Network.ConnectionManager;

public class Peer {

    public Peer(){}

    public int send_message(String msg, int conversation_id){
        return -1;
    }

    public int open_conversation(int conversation_id){
        return -1;
    }

    public void list_conversations(){

    }

    public void start_peer(){
        ConnectionManager.try_connect_to_peer("localhost:12345");
    }
}
