package peer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import peer.crypto.Stores;

public class TestMain {
	public static void main(String[] args) throws FileNotFoundException {
//		Stores.generateKeystore();

		Stores.tryLoadKeystore("test.jceks", "olaola");
	}
}
