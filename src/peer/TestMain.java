package peer;

import java.io.File;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;

import javax.crypto.SecretKey;

import dataTypes.PacketType;
import peer.crypto.MessageEncryption;
import peer.crypto.Stores;
import peer.network.Packet;

public class TestMain {
	public static void main(String[] args) throws Exception {
		//TestPacketSerialization();
		testPacketEncryption();
	}

	public static void TestPacketSerialization() {
		Packet p1 = new Packet("Test", "This is a test", PacketType.MSG);

		byte[] pBytes = p1.getBytes();

		Packet p2 = new Packet(pBytes);

		assert p1.equals(p2) : "Packets do not match deserialization failed";
		System.out.println("Packets match");
	}

	public static void testPacketEncryption() throws Exception {
		
		String msg = "This is a test";
		
		File f = new File("test.jceks");
		f.delete();

		// CREATE KEYSTORE
		String password = "olaola";
		KeyStore k = Stores.generateKeystore();
		//GENERATE SYMETRIC ENCRYPTION KEY
		System.out.println("> Generating encryption key");
		SecretKey sk = MessageEncryption.generatePBKDF2(password);
		//ENCRYPT MESSAGE
		System.out.println("> Encrypting packet");
		String encMsg = MessageEncryption.encriptDataWithSymetricKey(sk, msg.getBytes());
		//CREATE PACKET WITH ENCRYPTED MESSAGE
		Packet p1 = new Packet("Test", encMsg, PacketType.MSG);
		
		byte[] pBytes = p1.getBytes();

		System.out.println("> Proceding to assymetric encryption");
		System.out.println("> Retrieving public and private keys");

		PublicKey pk = k.getCertificate("test").getPublicKey();
		PrivateKey prk = (PrivateKey) k.getKey("test", password.toCharArray());

		assert pk != null : "Public key is null";
		assert prk != null : "Private key is null";
		//ENCRYPT PACKET WITH ASSYMETRIC CRYPTOGRAPHY
		String assEncMsg = MessageEncryption.encrypt(pBytes, pk);

		System.out.printf("Assymetrically encrypted packet > %s\n", assEncMsg);
		//DECRYPT PACKET (NOT MESSAGE)
		byte[] assDecMsg = MessageEncryption.decrypt(assEncMsg, prk);
		Packet p2 = new Packet(assDecMsg);
		
		assert p1.equals(p2) : "Packets do not match decryption failed";
		//DECRYPT MESSAGE
		System.out.println("> Decrypting message");
		byte[] dataBytes = p2.get_data().getBytes(); // encrypted data bytes
		String decMsg = new String(MessageEncryption.decriptDataWithSymetricKey(sk, dataBytes));

		assert decMsg.equals(msg) : "Messages do not match, message decryption FAILED";
		System.out.printf("Messages match > Original Message content: %s | Decrypted Message: %s\n", msg, decMsg);
		System.out.println("TEST SUCESS");
	}

	
	
//	public static void TestEncryption() throws Exception {
//		File f = new File("test.jceks");
//		f.delete();
//
//		String password = "olaola";
//		KeyStore k = Stores.generateKeystore();
//
//		System.out.println("> Generating encryption key");
//		SecretKey sk = MessageEncryption.generatePBKDF2(password);
//		System.out.println("> Saving encryption key");
//		Stores.saveKeyToKeyStore(k, password, sk, "test-ass", "test.jceks"); // alias needs to be different than the
//																				// initial keys
//
//		String name = "AAA";
//		String msg = "BBB";
//		System.out.println("> Retrieving encryption key");
//		SecretKey key = (SecretKey) k.getKey("test-ass", password.toCharArray());
//
//		assert sk.equals(key) : "Keys do not match";
//
//		System.out.println("> Building packet");
//		String packet = name + "{@}" + msg + "{@}MSG";
//
//		System.out.println("> Encrypting packet");
//		String encMsg = MessageEncryption.encriptDataWithSymetricKey(key, packet.getBytes());
//
//		System.out.println("> Decrypting packet");
//		String decMsg = new String(MessageEncryption.decriptDataWithSymetricKey(key, encMsg.getBytes()));
//
//		System.out.printf("Encrypted packet > %s\n", encMsg);
//		System.out.println(decMsg);
//		System.out.println(packet);
//
//		assert decMsg.equals(packet) : "Messages do not match";
//
////			System.out.printf("Original packet > %s\n", packet);
////			System.out.printf("Decrypted packet > %s\n", decMsg)		
//
//		// >>>>>>>>>>>>>>>>>Assymetric Encryption<<<<<<<<<<<<<<<<<<<<<<
//
//		System.out.println("> Proceding to assymetric encryption");
//		System.out.println("> Retrieving public and private keys");
//
//		PublicKey pk = k.getCertificate("test").getPublicKey();
//		PrivateKey prk = (PrivateKey) k.getKey("test", password.toCharArray());
//
//		assert pk != null : "Public key is null";
//		assert prk != null : "Private key is null";
//
//		String assEncMsg = MessageEncryption.encrypt(packet, pk);
//
//		System.out.printf("Assymetrically encrypted packet > %s\n", assEncMsg);
//		String assDecMsg = MessageEncryption.decrypt(assEncMsg, prk);
//
//		assert assDecMsg.equals(packet) : "Packets do not match";
//	}
}
