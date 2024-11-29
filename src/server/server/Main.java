package server.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.Scanner;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import client.peer.Peer;
import common.Stores;
import server.crypto.Encryption;
import server.messages.MessageReader;
import server.messages.ServerFiles;
import server.network.ConnectionManager;

public class Main {

	private volatile static boolean running = true;
	private static KeyStore keyStore;
	private static KeyStore trustStore;
	
	private static ServerSocket serverSocket;
	
	private static KeyManager[] keyManagers;
	private static TrustManager[] trustManagers;
	
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
	
	public static void main(String[] args) {

		//alias, port, keystore, password keystore,trust
		
		if(args.length != 5) {
			System.out.println("Missing arguments");
			System.out.println("<alias> <port> <keystore-path> <keystore-password> <truststore-path>");
			//print help menu
			return;
		}
		
		String aliasStr = args[0];
		String portStr = args[1];
		String ksPath = args[2];
		String ksPassword = args[3];
		String tsPath = args[4];
		
		System.out.printf("%s | %s | %s | %s | %s\n", aliasStr, portStr, ksPath, ksPassword, tsPath);
		
		int in_port = Integer.parseInt(portStr);
		keyStore = Stores.tryLoadKeystore(ksPath, ksPassword);
		trustStore = Stores.tryLoadTrustStore(tsPath, "");
		
		createTrustManager(ksPassword, keyStore, trustStore);
		
		ServerFiles.createDirs();
		Encryption.setConfig(aliasStr, ksPassword, keyStore, trustStore);
		
		serverSocket = ConnectionManager.createServerSocket(in_port, keyStore, keyManagers, trustManagers);
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
