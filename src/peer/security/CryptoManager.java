package peer.security;

import java.io.UnsupportedEncodingException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class CryptoManager {

	private static final int KEY_SIZE = 2048;
	private static final int ITERATIONS = 2048;

	/**
	 * Generates a salt based of a password
	 * 
	 * @param password the password to used for the salt generation
	 * @return the salt or null if there was an error
	 */
	private static byte[] deriveSaltFromPassword(String password) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return digest.digest(password.getBytes("UTF-8"));
		} catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
			return null;
		}
	}

	/**
	 * Derives a key from a password and salt
	 * @param password the password to be used
	 * @param salt the salt to be used
	 * @return the generated key or null in case of error
	 */
	private static byte[] deriveKeyFromPassword(String password, byte[] salt){
		try {
			PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_SIZE);
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			return skf.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			return null;	
		}
	}

	
	public static KeyPair generateRSAKeyPairFromPassword(String password) {
		byte[] salt = deriveSaltFromPassword(password);
		byte[] seed = deriveKeyFromPassword(password, salt);

		if(seed == null || salt == null) {
			System.err.println("Error deriving parameters for key generation");
			return null;
		}
		
		try {
			KeyPairGenerator keyPairGen= KeyPairGenerator.getInstance("RSA");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			random.setSeed(seed); // Set the fixed seed for deterministic key generation

			keyPairGen.initialize(KEY_SIZE, random);
			return keyPairGen.generateKeyPair(); // This key pair will always be the same for the same password
		} catch (NoSuchAlgorithmException e) {
			System.err.println("Error generating key pair");
			return null;
		}
	}

}
