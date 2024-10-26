package peer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.util.Base64;
import java.util.Scanner;

import javax.crypto.SecretKey;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import dataTypes.PacketType;
import peer.crypto.MessageEncryption;
import peer.crypto.Stores;
import peer.messages.MessageLogger;
import peer.network.ConnectionManager;
import peer.network.Packet;
import peer.threads.ConnectionAcceptorThread;

public class Peer {
	private String name;
	private final int in_port;
	private final int out_port;
	private ServerSocket peer_in;
	private Socket peer_out;
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
				
				PublicKey pk;
				SecretKey k;
				String encMsg;
				String packet = name + "{@}" + msg + "{@}MSG";
				
				if((k = checkForEncryptionKey(sc, alias, password)) == null) {
					return;
				}
				
				if((encMsg = MessageEncryption.encriptDataWithSymetricKey(k, packet.getBytes())) == null) {
					return;
				}
				
				if((pk = Stores.retrievePublicKey(trustStore, alias)) == null) {
					return;
				}
				
				if((encMsg = MessageEncryption.encrypt(encMsg, pk))== null) {
					return;
				}
				
				
				// this could be changed later on if a server is used to store non delivered
				// messages
				if (send_message(encMsg, in, out) == -1) {
					is_writing = false;
					System.out.println("Error sending message or message was blank/empty");
				}
				break;
			}
		}

		try {
			out.close();
			in.close();
		}catch (IOException e) {
			System.out.println("Error closing output and input buffers");
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
	private int send_message(String msg, ObjectInputStream in, ObjectOutputStream out) {
		if (msg == null || msg.isEmpty() || msg.isBlank()) {
			System.out.println("No message was provided");
			return -1;
		}

		if (peer_out == null) {
			System.out.println("Could not send message beacause you're not connected to a peer");
			return -1;
		}

		//TODO find a way to get the alias of the key and password for the keyStore
		String alias = ""; // alias of the person the message is going to 
		String password = ""; // password of the keyStore
		
		
		//Packet p = new Packet(name, msg, PacketType.MSG);
		ConnectionManager.sendPacket(msg, in, out);

		return 0;
	}
	
	
	private SecretKey checkForEncryptionKey(Scanner sc, String alias, String password) {
		try {
			
			SecretKey k = (SecretKey) keyStore.getKey(alias, password.toCharArray()); 
			
			if(k == null) {
				System.out.print("> Password (PBE): \n");
				String password4Key = sc.nextLine();
				
				k = MessageEncryption.generatePBKDF2(password4Key);
				password4Key = "";
			}
			
			return k;
		} catch (UnrecoverableKeyException | KeyStoreException |NoSuchAlgorithmException e) {
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
		ConnectionAcceptorThread connectionAcceptorThread = new ConnectionAcceptorThread(name, running, peer_in);

		// Start the thread that accepts connections
		connectionAcceptorThread.start();
	}

	public void close() {
		ConnectionManager.close_socket(peer_out);
		ConnectionManager.close_socket(peer_in);
	}

}