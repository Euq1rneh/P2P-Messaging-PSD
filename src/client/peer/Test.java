package client.peer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import client.peer.crypto.ShamirScheme;
import common.EncryptedPacket;
import common.Packet;
import common.PacketType;

public class Test {

	public static void main(String[] args) {

		testWrapUnwrap();
//		doIt();
	}

	public static KeyStore tryLoadKeystore(String path, String keystorePassword) {

		try (FileInputStream fis = new FileInputStream(path)) {
			KeyStore keystore = KeyStore.getInstance("JCEKS");
			keystore.load(fis, keystorePassword.toCharArray());
			return keystore;
		} catch (FileNotFoundException e) {
			System.out.println("Could not find the keystore at the specified path");
			return null;
		} catch (IOException e) {
			System.out.println("Error while trying to read keystore file");
			e.printStackTrace();
			return null;
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Could not find the specified algorithm");
			return null;
		} catch (CertificateException e) {
			System.out.println("Could not load a certificate from the keystore");
			return null;
		} catch (KeyStoreException e) {
			System.out.println("No provider supports a KeyStoreSpi implementation for JCEKS");
			return null;
		}
	}

	public static EncryptedPacket encryptPacket(Packet packet, PublicKey publicKey) {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(256);
			SecretKey aesKey = keyGen.generateKey();

			byte[] packetBytes = packet.getBytes();

			Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
			byte[] iv = new byte[12]; // Recommended GCM IV length
			SecureRandom secureRandom = new SecureRandom();
			secureRandom.nextBytes(iv);
			GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128-bit authentication tag

			aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
			byte[] encryptedPacketBytes = aesCipher.doFinal(packetBytes);

			Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
			rsaCipher.init(Cipher.WRAP_MODE, publicKey);
			byte[] encryptedAESKey = rsaCipher.wrap(aesKey);

			return new EncryptedPacket(encryptedPacketBytes, encryptedAESKey, iv);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Error encrypting packet");
		return null;
	}
	
	public static Packet decryptPacket(EncryptedPacket encryptedPacket, PrivateKey privateKey) {
		try {
			// Step 1: Decrypt the AES key with RSA
			Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
			rsaCipher.init(Cipher.UNWRAP_MODE, privateKey);
			SecretKey aesKey = (SecretKey) rsaCipher.unwrap(encryptedPacket.getEncryptedAESKey(), "AES",
					Cipher.SECRET_KEY);

			// Step 2: Decrypt packet data with AES-GCM
			Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
			GCMParameterSpec gcmSpec = new GCMParameterSpec(128, encryptedPacket.getIv());
			aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

			byte[] decryptedPacketBytes = aesCipher.doFinal(encryptedPacket.getEncryptedData());

			return new Packet(decryptedPacketBytes);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private static void testWrapUnwrap() {
		try {
			KeyStore keyStore = tryLoadKeystore("keystores/oracleServer-keystore.jceks", "oracleServer");
			KeyStore trustStore = tryLoadKeystore("truststores/truststore.jceks", "");

			PrivateKey prk = (PrivateKey) keyStore.getKey("oracleServer", "oracleServer".toCharArray());
			PublicKey pk = trustStore.getCertificate("oracleServer").getPublicKey();
			
			Packet p = new Packet("aaa", "ola", PacketType.AVAILABLE_FILES);
			
			EncryptedPacket ep = encryptPacket(p, pk);
			p = decryptPacket(ep, prk);
			
			if(p.get_data().equals("ola")) {
				System.out.println("AAAAAAAAAAAAAAAA");
			}else {
				System.out.println("BBBBBBBBBBBBBBB");
			}
			

		} catch (Exception e) {
			e.printStackTrace();
		}
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
		// b64 encode parts
		for (Map.Entry<Integer, byte[]> entry : parts.entrySet()) {
			Integer key = entry.getKey();
			byte[] val = entry.getValue();

			b64Parts.put(key, Base64.getEncoder().encodeToString(val));
		}
		// send to server
		// retrieve from server
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
