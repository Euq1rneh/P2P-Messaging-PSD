package client.dataTypes;

public enum PacketType {
    MSG,    //normal message
    ACK,    //MSG acknowledgment
    RET_FILE, //retrieve file
    CLS,    //close
}