package Peer.Network;

import Peer.Data.PacketType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Packet implements Serializable {

    private final String sender;
    private final String data;
    private String timestamp;

    private PacketType packetType;

    public Packet(String sender, String data, PacketType packetType){
        this.sender = sender;
        this.data = data;
        this.packetType = packetType;

        set_timestamp();
    }

    public void set_timestamp(){
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        timestamp = now.format(formatter);
    }

    public String get_sender(){
        return sender;
    }

    public String get_data(){
        return data;
    }

    public PacketType get_packet_type(){
        return get_packet_type();
    }
}
