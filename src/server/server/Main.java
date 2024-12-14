package server.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.Properties;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import common.ByteArray;
import common.Stores;
import server.crypto.Encryption;
import server.messages.MessageReader;
import server.messages.ServerFiles;
import server.messages.ServerMaps;
import server.network.ConnectionManager;

public class Main {

	private volatile static boolean running = true;
	private static KeyStore keyStore;
	private static KeyStore trustStore;
	
	private static ServerSocket serverSocket;
	
	private static KeyManager[] keyManagers;
	private static TrustManager[] trustManagers;
	private static boolean hasINIFile;
	private static String keystorePath;
	private static String password;
	private static String truststorePath;
	private static String truststorePassword;
	private static String username;
	private static int port;
	
	private static HashMap<String, HashMap<ByteArray, ByteArray>> searchMaps = new HashMap<String, HashMap<ByteArray,ByteArray>>();
	
	/**
	 * Reads the INI file that contains the necessary values to start the program
	 * automatically
	 */
	private static boolean readConfigFromINI() {
		File iniFile = new File("config.ini");

		System.out.println("> Checking for INI file...");

		if (iniFile.exists()) {
			hasINIFile = true;
			System.out.println("> INI file found reading properties...");
			Properties properties = new Properties();
			try (FileInputStream fis = new FileInputStream(iniFile)) {
				properties.load(fis);

				// Read the values from the properties file
				keystorePath = properties.getProperty("keystorePath");
				password = properties.getProperty("keystorePassword");
				truststorePath = properties.getProperty("truststorePath");
				truststorePassword = properties.getProperty("truststorePassword");

				// this field is not mandatory
				if (truststorePassword == null) {
					truststorePassword = "";
				}

				username = properties.getProperty("username");
				String portString = properties.getProperty("port");

				// Convert port to an integer (handle potential exception)
				if (portString != null) {
					try {
						port = Integer.parseInt(portString);
					} catch (NumberFormatException e) {
						System.err.println("Invalid port number in config file.");
						port = -1; // Set port to an invalid value to indicate error
						return false;
					}
				}

				// If any of the required values are missing, print an error
				if (keystorePath == null || password == null || truststorePath == null || username == null
						|| port == -1) {
					System.err.println("Missing required configurations in the config file.");
					return false;
				}
			} catch (IOException e) {
				System.err.println("Error reading the configuration file: " + e.getMessage());
				return false;
			}

			return true;
		} else {
			System.out.println("> INI file not found");
			return false;
		}
	}
	
	public static void createTrustManager(String kpassword, KeyStore keystore, KeyStore truststore) {
		KeyManagerFactory keyManager;
		try {
			keyManager = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManager.init(keystore, kpassword.toCharArray());
			keyManagers = keyManager.getKeyManagers();

			TrustManagerFactory trustManager = TrustManagerFactory
					.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManager.init(truststore);
			trustManagers = trustManager.getTrustManagers();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}

	}
	
	public static HashMap<ByteArray, ByteArray> getSearchMap(String username){
		HashMap<ByteArray, ByteArray> map = searchMaps.get(username);
		
		if(map == null) {
			map = new HashMap<ByteArray, ByteArray>();
			searchMaps.put(username, map);
		}
		return map;
	}
	
	public static void main(String[] args) {

		//alias, port, keystore, password keystore,trust
		if(args.length != 5) {
			readConfigFromINI();
			if(!hasINIFile) {
				System.out.println("> Missing arguments");
				System.out.println(" <alias> <port> <keystore-path> <keystore-password> <truststore-path>");
				return;	
			}
		}else {
			username = args[0];
			String portStr = args[1];
			keystorePath = args[2];
			password = args[3];
			truststorePath = args[4];
			
			port = Integer.parseInt(portStr);
		}
		
		keyStore = Stores.tryLoadKeystore(keystorePath, password);
		trustStore = Stores.tryLoadTrustStore(truststorePath, "");
		createTrustManager(password, keyStore, trustStore);
		
		ServerFiles.createDirs();
		Encryption.setConfig(username, password, keyStore, trustStore);
		
		serverSocket = ConnectionManager.createServerSocket(port, keyStore, keyManagers, trustManagers);
		System.out.println("Started backup server");
		while (!serverSocket.isClosed()) {
			try {
				SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
				System.out.println("New client connection");
				new Thread(new MessageReader(clientSocket, running)).start();
			} catch (SocketException e) {
				// this exception hopefully will only be thrown when quiting the program
				// so there is no need to handle the error
			} catch (IOException e) {
				System.out.println("Error accepting connection");
				e.printStackTrace();
			}
		}
	}

}
