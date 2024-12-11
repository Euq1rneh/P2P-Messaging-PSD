package client.peer;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import client.peer.crypto.ShamirScheme;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		doIt();
	}

	
	private static void doIt() {
	    final ShamirScheme scheme = new ShamirScheme(new SecureRandom(), 5, 3);
	    String msg = "ola";
	    byte[] encryptedMsg = msg.getBytes();
	    // equivalent to encFile
	    String b64Msg = Base64.getEncoder().encodeToString(encryptedMsg);
	    
	    final byte[] secret = b64Msg.getBytes(); 
	    final Map<Integer, byte[]> parts = scheme.split(secret);
	    
	    System.out.println(parts.toString());
	    
	    HashMap<Integer, String> b64Parts = new HashMap<Integer, String>();
	    //b64 encode parts
	    for (Map.Entry<Integer, byte[]> entry : parts.entrySet()) {
			Integer key = entry.getKey();
			byte[] val = entry.getValue();
			
			b64Parts.put(key, Base64.getEncoder().encodeToString(val));
		}
	    //send to server
	    //retrieve from server
	    Map<Integer, byte[]> partialParts = new HashMap<Integer, byte[]>();
	    
	    for (Map.Entry<Integer, String> entry : b64Parts.entrySet()) {
			Integer key = entry.getKey();
			String val = entry.getValue();
			
			partialParts.put(key, Base64.getDecoder().decode(val));
		}
	    
	    final byte[] recovered = scheme.join(partialParts);
	    final byte[] recoveredB64 = Base64.getDecoder().decode(recovered);
	    System.out.println(new String(recoveredB64));
	  } 
}
