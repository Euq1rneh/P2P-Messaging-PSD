package client.peer.network;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import common.EncryptedPacket;

public class ConnectionManager {

	public static ServerSocket peer_server(int port) {
		try {
			return new ServerSocket(port);
		} catch (IOException e) {
			System.out.println("<-----Error creating peer input socket----->");
			return null;
		}
	}

	public static ServerSocket createServerSocket(int port, KeyStore store, KeyManager[] keyManagers, TrustManager[] trustManagers) {	
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagers, trustManagers, null);

			SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();
			return socketFactory.createServerSocket(port);
		} catch (NoSuchAlgorithmException 
				| KeyManagementException
				| IOException e) {
			System.out.println("<-----Error creating peer input socket----->");
			e.printStackTrace();
			return null;
		}
	}
	
	private static SSLSocket peerClient(KeyStore keyStore, KeyStore trustStore, KeyManager[] keyManagers, TrustManager[] trustManagers, String address, int port) {
		
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagers, trustManagers, null);

			SSLSocketFactory socketFactory = sslContext.getSocketFactory();
			SSLSocket clientSocket = (SSLSocket) socketFactory.createSocket(address, port);
			
			return clientSocket;
		} catch (NoSuchAlgorithmException e) {
			return null;
		} catch (KeyManagementException e) {
			return null;
		} catch (UnknownHostException e) {
			return null;
		} catch (IOException e) {
			return null;
		}
		
	}

	/**
	 * Closes the socket passed as an argument
	 * 
	 * @param socket the socket to close
	 */
	public static void close_socket(Closeable socket) {
		if (socket == null) {
			return;
		}

		try {
			socket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Tries to establish a connection to a peer
	 * 
	 * @param peer_address the ip address of the peer
	 * @param peer_port    the port of the peer
	 * @return the socket that allows communication with the peer or null if there
	 *         was an error
	 */
	public static SSLSocket try_connect_to_peer(KeyStore keyStore, KeyStore trustStore, KeyManager[] keyManagers, TrustManager[] trustManager, String peer_address, int peer_port) {
		//return peer_client(peer_address, peer_port);
		return peerClient(keyStore, trustStore, keyManagers, trustManager, peer_address, peer_port);
	}

	/**
	 * Sends a packet to a peer with the specified socket
	 * 
	 * @param packet the packet to send
	 * @return 0 if the sent packet was received -1 otherwise
	 */
	public static EncryptedPacket sendPacket(EncryptedPacket packet, ObjectInputStream in, ObjectOutputStream out) {
		try {
			if(packet == null) {
				System.out.println("Packet is null, nothing to send");
				return null;
			}
			
			out.writeObject(packet);
			// Flush the stream to make sure the data is sent
			// out.flush();
//			System.out.println("Sent packet");

			// program can be blocked here if an ack packet is never received
			EncryptedPacket encAck = (EncryptedPacket) in.readObject();
			
			if (encAck == null) {
				System.out.println("Did not receive ACK packet");
				return null;
			}
			
			return encAck;
		} catch (IOException e) {
			System.out.println("Error sending packet: " + e.getMessage());
			return null;
		} catch (ClassNotFoundException e) {
			return null;
		}
	}
}