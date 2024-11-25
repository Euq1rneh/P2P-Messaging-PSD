package common;

public enum PacketType {
	MSG, 		// normal message
	ACK, 		// MSG acknowledgment
	RET_FILE, 	// retrieve file
	BACKUP, 	// backup file
	OP_ERROR, 	// operation error
	CLS, 		// close
}