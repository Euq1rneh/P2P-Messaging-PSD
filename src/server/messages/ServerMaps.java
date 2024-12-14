package server.messages;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import common.ByteArray;

public class ServerMaps {
	
	private static final String hmac_alg = "HmacSHA1";
	
	private Map<ByteArray,ByteArray> index;
	private Mac hmac;
	
	
	public ServerMaps(HashMap<ByteArray,ByteArray> map) {
		index = map;
		
		//This should not be in the constructor
		try {
			hmac = Mac.getInstance(hmac_alg);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	public void update (ByteArray label, ByteArray value) {
		index.put(label, value);
		printIndex();
	}
	
	public List<byte[]> search(byte[] k1) throws InvalidKeyException {
	    int c = 0;
	    List<byte[]> encryptedResults = new LinkedList<>();

	    hmac.init(new SecretKeySpec(k1, hmac_alg));
	    ByteArray value;
	    do {
	        byte[] l = hmac.doFinal(ByteBuffer.allocate(4).putInt(c).array());
	        ByteArray label = new ByteArray(l);
	        value = index.get(label);

	        if (value != null) {
	        	System.out.println("Found value corresponding to search term");
	            encryptedResults.add(value.getArr());
	            c++;
	        }
	    } while (value != null);

	    System.out.println("Returning results");
	    return encryptedResults;
	}
	
	public void printIndex() {
	    for (Map.Entry<ByteArray, ByteArray> entry : index.entrySet()) {
	        System.out.println("Label: " + Base64.getEncoder().encodeToString(entry.getKey().getArr()) +
	                           ", Encrypted Filename: " +
	                           Base64.getEncoder().encodeToString(entry.getValue().getArr()));
	    }
	}
}
