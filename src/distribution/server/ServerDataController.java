package distribution.server;

import java.util.HashMap;

public class ServerDataController {

	private HashMap<String, String> onlinePeers;
	private HashMap<String, String> peerPKs; // trocar segundo membro do hashmap para ser uma pk
	
	public ServerDataController() {
		onlinePeers = new HashMap<String, String>();
		peerPKs = new HashMap<String, String>();
	}
	
	public int add_peer(String name, String address) {
		if(onlinePeers.containsKey(name)) {
			return -1;
		}
		onlinePeers.put(name, address);
		return 0;
	}
	
	public String retrieve_peer_address(String name) {
		return onlinePeers.get(name); //can return null
	}
	
	public int add_pk(String name, String pk) {
		if(peerPKs.containsKey(name)) {
			return -1;
		}
		peerPKs.put(name, pk);
		return 0;
	}
	
	public String retrieve_peer_pk(String name) {
		return peerPKs.get(name); //can return null
	}
}
