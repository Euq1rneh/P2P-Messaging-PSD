package peer.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import peer.network.EncryptedPacket;
import peer.network.Packet;

public class HybridEncryption {

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
	        return null;
	    }
	 
	 public static Packet decryptPacket(EncryptedPacket encryptedPacket, PrivateKey privateKey) {
	        try {
	            // Step 1: Decrypt the AES key with RSA
	            Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
	            rsaCipher.init(Cipher.UNWRAP_MODE, privateKey);
	            SecretKey aesKey = (SecretKey) rsaCipher.unwrap(encryptedPacket.getEncryptedAESKey(), "AES", Cipher.SECRET_KEY);

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
}
