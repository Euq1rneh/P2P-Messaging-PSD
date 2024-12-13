package common;

public enum PacketType {
	MSG, // normal message
	ACK, // MSG acknowledgment
	RET_FILE, // retrieve file
	BACKUP, // backup file
	AVAILABLE_FILES, // check for available files in servers
	OP_ERROR, // operation error
	SEARCH, // when searching keywords
	ADD_KEYWORD, // adding new keywords
	CLS, // close
}