package dataTypes;

public enum PacketType {
    MSG,    //normal message
    ACK,    //MSG acknowledgment
    CLS,    //close
    PEERS,	//list of connected peers(to server)
    PK_RET, //retrieve pk from server
}