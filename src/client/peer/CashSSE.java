package client.peer;

import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CashSSE {
	
	private static final String hmac_alg = "HmacSHA1";
	private static final String cipher_alg = "AES";
	private static final SecureRandom rndGenerator = new SecureRandom();
	private static Mac hmac;
	private static Cipher aes;
	private static IvParameterSpec iv;	//fixed iv for simplicity
	
	private static Server server;
	private static Client client;

	
	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		hmac = Mac.getInstance(hmac_alg);
		aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
		
		System.out.println("Starting up the Health Record DB...");
		client = new Client();
		server = new Server();
		
		
		System.out.println("Finished! Populating the DB...");
		
		client.update("bob", "doc1");
		client.update("cancer", "doc1");
		client.update("liver", "doc1");
		
		client.update("alice", "doc2");
		client.update("avc", "doc2");
		client.update("heart", "doc2");
		
		client.update("charlie", "doc3");
		client.update("trauma", "doc3");
		client.update("brain", "doc3");
		client.update("liver", "doc3");
		
		System.out.println("Finished! Searching for bob:");
		List<String> ids = client.search("bob");
		for(String id: ids)
			System.out.println(id);
		
		System.out.println("Finished! Searching for liver:");
		ids = client.search("liver");
		for(String id: ids)
			System.out.println(id);
	}
	

	private static class Client {

		
		private HashMap<String,Integer> counters;
		private SecretKeySpec sk;
		
		public Client() throws NoSuchAlgorithmException, NoSuchPaddingException {
			byte[] sk_bytes = new byte[20];
			byte[] iv_bytes = new byte[16];
			rndGenerator.nextBytes(sk_bytes);
			rndGenerator.nextBytes(iv_bytes);
			sk = new SecretKeySpec(sk_bytes, hmac_alg);
			iv = new IvParameterSpec(iv_bytes);
			counters = new HashMap<>(100);
		}
		
		public void update(String keyword, String docName) 
		        throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		    // Generate keys k1 and k2 from keyword and sk by using a PRF
		    hmac.init(sk);
		    SecretKeySpec k1 = new SecretKeySpec(hmac.doFinal((keyword + "1").getBytes()), hmac_alg);
		    SecretKeySpec k2 = new SecretKeySpec(hmac.doFinal((keyword + "2").getBytes()), 0, 16, cipher_alg);

		    // Get the counter c for keyword from counters, or set it to 0 if not found
		    int c = counters.getOrDefault(keyword, 0);

		    // Calculate the index label l through a PRF and using k1 as key and c as plaintext
		    hmac.init(k1);
		    byte[] l = hmac.doFinal(ByteBuffer.allocate(4).putInt(c).array());

		    // Calculate the index value d through a symmetric-key cipher, using k2 as key and docName as plaintext
		    aes.init(Cipher.ENCRYPT_MODE, k2, iv);
		    byte[] d = aes.doFinal(docName.getBytes());

		    // Send l and d to the server to update the index
		    server.update(new ByteArray(l), new ByteArray(d));

		    // Increment counter c and update it in counters
		    counters.put(keyword, ++c);
		}

		public List<String> search(String keyword) 
		        throws InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		    // Generate k1 and k2 from keyword
		    hmac.init(sk);
		    SecretKeySpec k1 = new SecretKeySpec(hmac.doFinal((keyword + "1").getBytes()), hmac_alg);
		    SecretKeySpec k2 = new SecretKeySpec(hmac.doFinal((keyword + "2").getBytes()), 0, 16, cipher_alg);

		    // Query the server with k1
		    List<byte[]> encryptedResults = server.search(k1.getEncoded());

		    // Decrypt the results using k2
		    List<String> results = new LinkedList<>();
		    aes.init(Cipher.DECRYPT_MODE, k2, iv);
		    for (byte[] encryptedDocName : encryptedResults) {
		        byte[] docNameBytes = aes.doFinal(encryptedDocName);
		        results.add(new String(docNameBytes)); // Convert decrypted bytes back into a string
		    }

		    return results;
		}

		
	}
	
	private static class Server {
		
		private Map<ByteArray,ByteArray> index;
		
		public Server() {
			index = new HashMap<ByteArray,ByteArray>(1000);
		}
		
		public void update (ByteArray label, ByteArray value) {
			//update the index with a new entry <label, value>
			index.put(label, value);
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
		            encryptedResults.add(value.getArr());
		            c++;
		        }
		    } while (value != null);

		    return encryptedResults;  // Return the list of encrypted values.
		}


		
	}

	private static class ByteArray {
		
		private byte[] arr;

		public ByteArray(byte[] array) {
			arr = array;
		}
		
		public byte[] getArr() {
			return arr;
		}
		
		@Override
	    public boolean equals(Object obj) {
	        return obj instanceof ByteArray && Arrays.equals(arr, ((ByteArray)obj).getArr());
	    }
		
	    @Override
	    public int hashCode() {
	        return Arrays.hashCode(arr);
	    }
	}
	
}
