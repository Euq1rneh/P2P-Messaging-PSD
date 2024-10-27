package peer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import dataTypes.PacketType;
import peer.crypto.HybridEncryption;
import peer.crypto.MessageEncryption;
import peer.crypto.Stores;
import peer.messages.MessageLogger;
import peer.network.ConnectionManager;
import peer.network.EncryptedPacket;
import peer.network.Packet;
import peer.threads.ConnectionAcceptorThread;

public class Peer {
	private String name;
	private final int in_port;
	private final int out_port;
	private ServerSocket peer_in;
	private SSLSocket peer_out;
	// streams used for sending messages
	private ObjectOutputStream out;
	private ObjectInputStream in;

	private String[] conversations;

	private KeyStore keyStore;
	private KeyStore trustStore;
	private String password;

	KeyManager[] keyManagers;
	TrustManager[] trustManagers;

	public Peer(String name, int in_port, int out_port, KeyStore keyStore, KeyStore trustStore, String password) {
		this.name = name;
		this.in_port = in_port;
		this.out_port = out_port;
		this.keyStore = keyStore;
		this.trustStore = trustStore;
		this.password = password;
	}

	/**
	 * Connect to another peer using their Ip and port
	 * 
	 * @param address the ip address of the peer
	 * @param port    the port of the peer
	 */
	public int connect(String address, int port) {
//    	peer_out = ConnectionManager.try_connect_to_peer(address, port);

		peer_out = ConnectionManager.try_connect_to_peer(keyStore, trustStore, keyManagers, trustManagers, address,
				port);

		if (peer_out == null) {
			System.out.printf("Could not establish connection to peer (%s:%d)\n", address, port);
			return -1;
		}

		System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAA");

		try {

			out = new ObjectOutputStream(peer_out.getOutputStream());
			in = new ObjectInputStream(peer_out.getInputStream());
		} catch (IOException e) {
			System.out.println("Could not create communication streams");
			return -1;
		}

		return 0;
	}

	public void try_send_message(Scanner sc, String alias) {
		boolean is_writing = true;

		while (is_writing) {
			System.out.print("Message > ");
			String msg = sc.nextLine();

			switch (msg) {
			case ":b":
				is_writing = false;
				break;
			default:
				if (msg.isBlank() || msg.isEmpty()) {
					break;
				}

				EncryptedPacket encPacket = encryptPacket(alias, msg, PacketType.MSG);

				if (encPacket == null) {
					System.out.println("Error encrypting packet");
					is_writing = false;
					continue;
				}

				// this could be changed later on if a server is used to store non delivered
				// messages
				Packet ack;
				
				if ((ack = send_message(encPacket, in, out)) == null) {
					is_writing = false;
					System.out.println("Error sending message or message was blank/empty");
					continue;
				}
				MessageLogger.write_message_log(name + ": " + msg, ack.get_sender() + ".conversation");
				break;
			}
		}

		try {
			out.close();
			in.close();
		} catch (IOException e) {
			System.out.println("Error closing output and input buffers");
		}
	}

	public EncryptedPacket encryptPacket(String alias, String msg, PacketType type) {

		try {
			PublicKey pk = trustStore.getCertificate(alias).getPublicKey();

			if (pk == null) {
				System.out.println("Error retrieving assymetric keys");
				System.exit(-1);
			}
			
			Packet packet = new Packet(name, msg, type);
			EncryptedPacket encPacket = HybridEncryption.encryptPacket(packet, pk);

			return encPacket;
		} catch (KeyStoreException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Sends the specified message to the specified peer (address + port)
	 * 
	 * @param msg          the message to send
	 * @param peer_address the ip address of the peer that should receive the
	 *                     message
	 * @param peer_port    the port of the peer that should receive the message
	 * @return 0 if there was no error -1 otherwise
	 */
	private Packet send_message(EncryptedPacket encPacket, ObjectInputStream in, ObjectOutputStream out) {
		if (encPacket == null) {
			System.out.println("No message was provided");
			return null;
		}

		if (peer_out == null) {
			System.out.println("Could not send message beacause you're not connected to a peer");
			return null;
		}

		EncryptedPacket encAck;
		if ((encAck = ConnectionManager.sendPacket(encPacket, in, out)) == null) {
			System.out.println("Did not receive ACK packet");
			return null;
		}
		
		Packet ack = tryReadMessage(encAck);
		return ack;
	}

	/**
	 * Tries to read an encrypted message from a peer
	 * 
	 * @param message the encrypted message
	 */
	public Packet tryReadMessage(EncryptedPacket message) {

		try {
			PrivateKey prk = (PrivateKey) keyStore.getKey(name, password.toCharArray());
			Packet p = HybridEncryption.decryptPacket(message, prk);
			
			return p;
		} catch (UnrecoverableKeyException 
				| KeyStoreException 
				| NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	private SecretKey checkForEncryptionKey(Scanner sc, String alias, String password) {
		try {

			SecretKey k = (SecretKey) keyStore.getKey(alias, password.toCharArray());

			if (k == null) {
				System.out.print(">No key found creating new key...\n> Password (PBE): \n");
				String password4Key = sc.nextLine();

				k = MessageEncryption.generatePBKDF2(password4Key);
				password4Key = "";
			}

			return k;
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Open a conversation with a peer if a conversation does not exist it creates a
	 * new one. This method is also responsible for connecting to the peer?????
	 * 
	 * @param conversation_id the id of the conversation to open
	 * @return 0 if could open the conversation -1 in case of error
	 */
	public int open_conversation(int conversation_id) {
		String conversation = MessageLogger.read_message_log(conversations[conversation_id] + ".conversation");

		if (conversation == null) {
			return -1;
		}

		System.out.println(conversation);

		return 0;
	}

	public void list_conversations() {
		conversations = MessageLogger.get_conversations();
		System.out.println("----------- Conversations -----------");
		if (conversations == null) {
			System.out.println("No conversations were found.");
			System.out.println("Try starting a conversation by connecting to a peer and sending a message.");
			System.out.println("------------------------------------");
			return;
		}

		for (int i = 0; i < conversations.length; i++) {
			System.out.printf("%d. %s\n", i, conversations[i]);
		}
		System.out.println("------------------------------------");
	}

	public void createTrustManager(String kpassword, KeyStore keystore, KeyStore truststore) {
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

	/**
	 * Starts the peer by opening a socket that is responsible for accepting
	 * connections and reading the incoming messages
	 */
	public void start(boolean running, KeyStore keyStore, String password) {

		MessageLogger.build_conversation_dir();
		// peer_in = ConnectionManager.peer_server(in_port);
		peer_in = ConnectionManager.createServerSocket(in_port, keyStore, keyManagers, trustManagers);

		if (peer_in == null) {
			System.out.println("Error while trying to initialize peer");
			System.exit(-1);
		}

		// Create a thread to handle accepting connections
		ConnectionAcceptorThread connectionAcceptorThread = new ConnectionAcceptorThread(this, running);

		// Start the thread that accepts connections
		connectionAcceptorThread.start();
	}

	public void close() {
		ConnectionManager.close_socket(peer_out);
		ConnectionManager.close_socket(peer_in);
	}

	public String getName() {
		return name;
	}

	public ServerSocket getInputSocket() {
		return peer_in;
	}
}