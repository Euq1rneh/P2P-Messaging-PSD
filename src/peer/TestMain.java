package peer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;

import peer.crypto.Stores;

public class TestMain {
	public static void main(String[] args) throws FileNotFoundException, KeyStoreException {
		
		
		KeyStore k = Stores.generateKeystore();
		
		
		
		
	}
}
