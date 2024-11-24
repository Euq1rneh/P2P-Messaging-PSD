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
import server.messages.MessageReader;
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

		//port, keystore, password keystore,trust
		
		if(args.length != 4) {
			System.out.println("Missing arguments");
			//print help menu
			return;
		}
		
		int in_port = Integer.parseInt(args[0]);
		keyStore = Stores.tryLoadKeystore(args[1], args[2]);
		trustStore = Stores.tryLoadTrustStore(args[3], "");
		
		createTrustManager(args[2], keyStore, trustStore);
		
		//clear password????
		
		serverSocket = ConnectionManager.createServerSocket(in_port, keyStore, keyManagers, trustManagers);
		System.out.println("Started backup server");
		while (!serverSocket.isClosed()) {
			try {
				SSLSocket clientSocket = (SSLSocket) serverSocket.accept();

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
