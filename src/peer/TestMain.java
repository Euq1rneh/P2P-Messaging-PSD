package peer;

import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.SecretKey;

import peer.crypto.MessageEncryption;
import peer.crypto.Stores;

public class TestMain {
	public static void main(String[] args) throws Exception {
		File f = new File("test.jceks");
		f.delete();

		String password = "olaola";
		SecureRandom rd = new SecureRandom();
		KeyStore k = Stores.generateKeystore();

		byte[] salt = new byte[16];
		rd.nextBytes(salt);
		int iterations = rd.nextInt(100) + 1;
		System.out.println("> Generating encryption key");
		SecretKey sk = MessageEncryption.generatePBKDF2(password, salt, iterations);
		System.out.println("> Saving encryption key");
		Stores.saveKeyToKeyStore(k, password, sk, "test-ass", "test.jceks"); // alias needs to be different than the
																				// initial keys

		String name = "AAA";
		String msg = "BBB";
		System.out.println("> Retrieving encryption key");
		SecretKey key = (SecretKey) k.getKey("test-ass", password.toCharArray());

		assert sk.equals(key) : "Keys do not match";

		System.out.println("> Building packet");
		String packet = name + "{@}" + msg + "{@}MSG";

		System.out.println("> Encrypting packet");
		String encMsg = MessageEncryption.encriptDataWithSymetricKey(key, packet.getBytes());

		System.out.println("> Decrypting packet");
		String decMsg = new String(MessageEncryption.decriptDataWithSymetricKey(key, encMsg.getBytes()));

		System.out.printf("Encrypted packet > %s\n", encMsg);
		System.out.println(decMsg);
		System.out.println(packet);
		
		assert decMsg.equals(packet) : "Messages do not match";

//			System.out.printf("Original packet > %s\n", packet);
//			System.out.printf("Decrypted packet > %s\n", decMsg)		

		//>>>>>>>>>>>>>>>>>Assymetric Encryption<<<<<<<<<<<<<<<<<<<<<<
		
		System.out.println("> Proceding to assymetric encryption");
		System.out.println("> Retrieving public and private keys");

		PublicKey pk = k.getCertificate("test").getPublicKey();
		PrivateKey prk = (PrivateKey) k.getKey("test", password.toCharArray());

		assert pk != null : "Public key is null";
		assert prk != null : "Private key is null";

		String assEncMsg = MessageEncryption.encrypt(packet, pk);

		System.out.printf("Assymetrically encrypted packet > %s\n", assEncMsg);
		String assDecMsg = MessageEncryption.decrypt(assEncMsg, prk);

		assert assDecMsg.equals(packet) : "Packets do not match";
	}
}
