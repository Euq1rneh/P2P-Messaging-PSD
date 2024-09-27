package Peer.Network;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Packet implements Serializable {

    public String sender;
    public String message;
    public String timestamp;

    public Packet(String sender, String message){
        this.sender = sender;
        this.message = message;
    }

    public void set_timestamp(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        timestamp = now.format(formatter);
    }
}
