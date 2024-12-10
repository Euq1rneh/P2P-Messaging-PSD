package client.peer;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
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
	    final byte[] secret = "hello there".getBytes(StandardCharsets.UTF_8);
	    final Map<Integer, byte[]> parts = scheme.split(secret);
	    
	    
	    Map<Integer, byte[]> partialParts = new HashMap<Integer, byte[]>();
	    
	    partialParts.put(1, parts.get(1));
	    partialParts.put(2, parts.get(2));
//	    partialParts.put(4, parts.get(4));
	    
	    System.out.println(parts.toString());
	    System.out.println(partialParts.toString());
	    
	    final byte[] recovered = scheme.join(partialParts);
	    System.out.println(new String(recovered, StandardCharsets.UTF_8));
	  } 
}
