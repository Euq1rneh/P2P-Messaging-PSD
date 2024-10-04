package distribution.server;

import java.util.HashMap;

public class ServerDataController {

	private HashMap<String, String> onlinePeers;
	private HashMap<String, String> peerPKs; // trocar segundo membro do hashmap para ser uma pk
	
	public ServerDataController() {
		onlinePeers = new HashMap<String, String>();
		peerPKs = new HashMap<String, String>();
	}
	
	public void add_peer(String name, String address) {
		// make the necessary checks
		onlinePeers.put(name, address);
	}
	
	public String retrieve_peer_address(String name) {
		return onlinePeers.get(name); //can return null
	}
	
	public void add_pk(String name, String pk) {
		// make the necessary checks
		peerPKs.put(name, pk);
	}
	
	public String retrieve_peer_pk(String name) {
		return peerPKs.get(name); //can return null
	}
}
