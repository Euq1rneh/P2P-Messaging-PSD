package client.peer.crypto;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import common.EncryptedPacket;
import common.Packet;

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
			return null;
		}
	}

	public static byte[] readFileToByteArray(File file) throws IOException {
		try (FileInputStream fis = new FileInputStream(file); // FileInputStream to read the file
				ByteArrayOutputStream baos = new ByteArrayOutputStream()) { // ByteArrayOutputStream to accumulate bytes

			byte[] buffer = new byte[1024]; // Buffer for reading chunks of the file
			int bytesRead;
			while ((bytesRead = fis.read(buffer)) != -1) {
				baos.write(buffer, 0, bytesRead); // Write the bytes to ByteArrayOutputStream
			}

			return baos.toByteArray(); // Convert the ByteArrayOutputStream to byte[]
		}
	}

    public static String encryptFile(File f, PublicKey pk) {
        SecretKey aesKey;
        byte[] encryptedFileBytes;
        byte[] aesKeyBytes;

        try {
            // Step 1: Generate AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256); // 256-bit AES key
            aesKey = keyGen.generateKey();

            // Step 2: Read the file content
            byte[] fileBytes = readFileToByteArray(f);

            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
			byte[] iv = deriveIV(aesKey);
			GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv); // 128-bit authentication tag

			aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
			encryptedFileBytes = aesCipher.doFinal(fileBytes);

            // Step 4: Wrap the AES key with the public key (RSA encryption)
            aesKeyBytes = wrapAESKey(aesKey, pk);

        } catch (NoSuchAlgorithmException | IOException e) {
            System.out.println("Encryption failed");
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.out.println("Encryption error");
            e.printStackTrace();
            return null;
        }

        // Step 5: Base64 encode the AES key and the encrypted file content
        String base64EncryptedAESKey = Base64.getEncoder().encodeToString(aesKeyBytes);
        String base64EncryptedFile = Base64.getEncoder().encodeToString(encryptedFileBytes);

        // Step 6: Concatenate the Base64-encoded AES key and encrypted file content with '@'
        return base64EncryptedAESKey + "@" + base64EncryptedFile;
    }

	private static byte[] deriveIV(SecretKey aesKey) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest(aesKey.getEncoded());
		return Arrays.copyOf(hash, 12); // Use the first 12 bytes for GCM
	}

	public static File decryptFile(String conversationName, String encryptedFile, PrivateKey prKey) {
		String[] fileParts = encryptedFile.split("@");

		if (fileParts.length != 2) {
			System.out.println("Error decrypting file. File may have been corrupted");
			return null;
		}

		byte[] aesKeyByte = Base64.getDecoder().decode(fileParts[0]);
		byte[] fileByte = Base64.getDecoder().decode(fileParts[1]);

		byte[] decryptedFileBytes;
		SecretKey aesKey;

		try {
			aesKey = unwrapAESKey(aesKeyByte, prKey);
			Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
			GCMParameterSpec gcmSpec = new GCMParameterSpec(128, deriveIV(aesKey)); // iv is derived from the key's hash
			aesCipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

			decryptedFileBytes = aesCipher.doFinal(fileByte);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		File returnFile = new File("conversations/" + conversationName);

		try (FileOutputStream fos = new FileOutputStream(returnFile)) {
//			System.out.printf("Writing %s to file", decryptedFileBytes);
			fos.write(decryptedFileBytes);
		} catch (IOException e) {
			return null;
		}

		return returnFile;
	}

	public static byte[] wrapAESKey(SecretKey aesKey, PublicKey publicKey) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		cipher.init(Cipher.WRAP_MODE, publicKey);
		return cipher.wrap(aesKey);
	}

	// Method to unwrap an AES key with a private key
	public static SecretKey unwrapAESKey(byte[] wrappedKey, PrivateKey privateKey) throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		cipher.init(Cipher.UNWRAP_MODE, privateKey);
		return (SecretKey) cipher.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
	}
}
