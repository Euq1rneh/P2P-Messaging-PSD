package server.network;

import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;

import common.EncryptedPacket;

public class ConnectionManager {

	public static ServerSocket createServerSocket(int port, KeyStore store, KeyManager[] keyManagers, TrustManager[] trustManagers) {	
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagers, trustManagers, null);

			SSLServerSocketFactory socketFactory = sslContext.getServerSocketFactory();
			return socketFactory.createServerSocket(port);
		} catch (NoSuchAlgorithmException 
				| KeyManagementException
				| IOException e) {
			System.out.println("<-----Error creating server input socket----->");
			e.printStackTrace();
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
