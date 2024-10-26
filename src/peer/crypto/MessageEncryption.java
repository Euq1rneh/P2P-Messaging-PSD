package peer.crypto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MessageEncryption {

//	public static String encriptMessage(SecretKey k, String data) {
//
//		try {
//			byte[] toEncript = data.getBytes();
//			Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
//			SecureRandom random = new SecureRandom();
//
//			// generate iterations
//			int iterations = random.nextInt(101) + 1;
//			byte[] byteIterations = ByteBuffer.allocate(4).putInt(iterations).array();
//			// generate salt
//			byte[] salt = new byte[cipher.getBlockSize()];
//			random.nextBytes(salt);
//			// generate iv
//			byte[] iv = new byte[cipher.getBlockSize()];
//			random.nextBytes(iv);
//			
//			AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterations, new IvParameterSpec(iv));
//			cipher.init(Cipher.ENCRYPT_MODE, k, paramSpec);
//
//			byte[] encrypted = cipher.doFinal(toEncript);
//			byte[] result = new byte[byteIterations.length + salt.length + iv.length + encrypted.length];
//			
//			System.arraycopy(byteIterations, 0, result, 0, byteIterations.length);
//			System.arraycopy(salt, 0, result, byteIterations.length, salt.length);
//			System.arraycopy(iv, 0, result, byteIterations.length + salt.length, iv.length);
//			System.arraycopy(encrypted, 0, result, byteIterations.length + salt.length + iv.length, encrypted.length);
//
//			return Base64.getEncoder().encodeToString(result);
//
//		} catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException
//				| NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return null;
//	}
//	
//	public static byte[] decriptMessage(SecretKey k, String s) {
//
//		try {
//			byte[] encryptedMessage = Base64.getDecoder().decode(s.getBytes());
//			Cipher cipher = Cipher.getInstance("PBEWithHmacSHA256AndAES_128");
//
//			byte[] iterationBytes = new byte[4];
//			byte[] salt = new byte[cipher.getBlockSize()];
//			byte[] iv = new byte[cipher.getBlockSize()];
//
//			System.arraycopy(encryptedMessage, 0, iterationBytes, 0, iterationBytes.length);
//			System.arraycopy(encryptedMessage, iterationBytes.length, salt, 0, salt.length);
//			System.arraycopy(encryptedMessage, iterationBytes.length + salt.length, iv, 0, iv.length);
//
//			ByteBuffer buffer = ByteBuffer.wrap(iterationBytes);
//
//			// Retrieve the integer value from the ByteBuffer
//			int iterations = buffer.getInt();
//
//			AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterations, new IvParameterSpec(iv));
//			cipher.init(Cipher.DECRYPT_MODE, k, paramSpec);
//
//			byte[] encrypted = new byte[encryptedMessage.length - (iv.length + salt.length + iterationBytes.length)];
//			System.arraycopy(encryptedMessage, iv.length + salt.length + iterationBytes.length, encrypted, 0,
//					encrypted.length);
//			return cipher.doFinal(encrypted);
//
//		} catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException
//				| InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return null;
//	}
	
	public static String encriptDataWithSymetricKey(SecretKey k, byte[]data) {
	    try {
	        byte[] toEncrypt = data;
	        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        SecureRandom random = new SecureRandom();
	        
	        byte[] iv = new byte[cipher.getBlockSize()];
	        random.nextBytes(iv);
	        AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
	        
	        cipher.init(Cipher.ENCRYPT_MODE, k, paramSpec);
	        
	        byte[] encrypted = cipher.doFinal(toEncrypt);
	        byte[] result = new byte[iv.length + encrypted.length];
	        
	        System.arraycopy(iv, 0, result, 0, iv.length);
	        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
	        
	        return Base64.getEncoder().encodeToString(result);
	    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
	        // Handle exception appropriately
	        e.printStackTrace();
	    }
	    return null;
	}
	
	public static byte [] decriptDataWithSymetricKey(SecretKey k, byte[] encryptedData) {
	    try {
	        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
	        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
	        
	        // Extract IV from the encrypted data
	        byte[] iv = new byte[cipher.getBlockSize()];
	        System.arraycopy(encryptedBytes, 0, iv, 0, iv.length);
	        AlgorithmParameterSpec paramSpec = new javax.crypto.spec.IvParameterSpec(iv);
	        
	        cipher.init(Cipher.DECRYPT_MODE, k, paramSpec);
	        
	        return cipher.doFinal(encryptedBytes, iv.length, encryptedBytes.length - iv.length);
	    } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
	        // Handle exception appropriately
	        e.printStackTrace();
	    }
	    return null;
	}
	
	public static SecretKeySpec generatePBKDF2(String password, byte[] salt, int iterations) {
	    try {
	        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
	        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256); // 256-bit key length
	        SecretKey tmp = factory.generateSecret(spec);
	        return new SecretKeySpec(tmp.getEncoded(), "AES");
	    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
	        // Handle exception appropriately
	        e.printStackTrace();
	    }
	    return null;
	}
	
	public static byte[] wrapSecretKeyWithPublicKey(SecretKey k, PublicKey pk) {

		try {
			Cipher c = Cipher.getInstance("RSA");
			c.init(Cipher.WRAP_MODE, pk);

			return c.wrap(k);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
				| IllegalBlockSizeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}
	
	public static Key unwrapKey(byte[] wrappedKey, PrivateKey kr) {

		Cipher c;
		try {
			c = Cipher.getInstance("RSA");
			c.init(Cipher.UNWRAP_MODE, kr);
			return c.unwrap(wrappedKey, "AES", Cipher.SECRET_KEY);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}  
		
		return null;
	}
	
	
    public static String encrypt(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
        String encryptedBase64 = Base64.getEncoder().encodeToString(encryptedData);
        System.out.println("Encrypted (Base64): " + encryptedBase64);
        return encryptedBase64;
    }

    // Decrypt data using the private key
    public static String decrypt(String encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedData = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    // Sign the data using SHA256withRSA
    public static byte[] signData(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    // Verify the signature
    public static boolean verifySignature(byte[] data, byte[] signatureBytes, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(signatureBytes);
    }
}
