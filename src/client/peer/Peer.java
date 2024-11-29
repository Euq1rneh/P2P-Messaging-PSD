package client.peer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ResponseCache;
import java.net.ServerSocket;
import java.nio.file.FileSystems;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import client.peer.crypto.HybridEncryption;
import client.peer.messages.MessageLogger;
import client.peer.network.ConnectionManager;
import client.peer.threads.ConnectionAcceptorThread;
import common.EncryptedPacket;
import common.Packet;
import common.PacketType;

public class Peer {
	private String name;
	private final int in_port;
	private ServerSocket peer_in;
	private SSLSocket peer_out;
	// streams used for sending messages
	private ObjectOutputStream out;
	private ObjectInputStream in;

	private String[] conversations;

	private KeyStore keyStore;
	private KeyStore trustStore;
	private String password;

	private HashMap<Integer, String> serverAliases = new HashMap<Integer, String>();
	KeyManager[] keyManagers;
	TrustManager[] trustManagers;

	public Peer(String name, int in_port, KeyStore keyStore, KeyStore trustStore, String password) {
		this.name = name;
		this.in_port = in_port;
		this.keyStore = keyStore;
		this.trustStore = trustStore;
		this.password = password;

		serverAliases.put(0, "amazonServer");
		serverAliases.put(1, "oracleServer");
		serverAliases.put(2, "googleServer");
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

	private SSLSocket[] connectToBackupServer() {
		String[] serverAdresses = { "127.0.0.1:1111", "127.0.0.1:2222", "127.0.0.1:3333" };
		SSLSocket[] servers = new SSLSocket[serverAdresses.length];

		for (int i = 0; i < serverAdresses.length; i++) {
			String[] connectionArgs = serverAdresses[i].split(":");

			servers[i] = ConnectionManager.try_connect_to_peer(keyStore, trustStore, keyManagers, trustManagers,
					connectionArgs[0], Integer.parseInt(connectionArgs[1]));

			if (servers[i] == null) {
				System.out.println("Could not connect to server with address " + serverAdresses[i]);
			}
		}

		return servers;
	}

	/**
	 * Tries to retrieve a file from the backup servers
	 * 
	 * @param serverAlias  the server alias for key retrieval
	 * @param filename     the name of the file to retrieve
	 * @param outputStream the output stream of the socket connection to the server
	 * @param inputStream  the input stream of the socket connection to the server
	 * @return the file contents if it found the file null otherwise
	 */
	private String retrieveFile(String serverAlias, String filename, ObjectOutputStream outputStream,
			ObjectInputStream inputStream) {
		Packet p = new Packet(name, filename, PacketType.RET_FILE);

		String encData = null;
//		System.out.println("Searching for existing file in server " + serverAlias);

		EncryptedPacket encRequest = encryptPacket(serverAlias, p);
		
		try {

//			System.out.println("Sending file request");
			outputStream.writeObject(encRequest);

//			System.out.println("Waiting for server response...");
			EncryptedPacket encResponse = (EncryptedPacket) inputStream.readObject();

			if (encResponse.getEncryptedData() == null || encResponse.getEncryptedAESKey() == null
					|| encResponse.getIv() == null) {

				// server could not decode message
//				System.out.println("Server could not decode message from client");
				return null;
			}

			Packet response = tryReadMessage(encResponse);
			
//			System.out.println("Server response detected. Reading response...");
			
			if(response == null) {
//				System.out.println("Error while trying to read response from server");
				return null;
			}

			if ((encData = response.get_data()) != null) {
//				System.out.println("Server has backup file. Skipping other servers...");
				return encData;
			}

//			System.out.println("Server did not have backup file. Asking other servers...");

		} catch (IOException e) {
			System.out.println("Error with streams");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Could not cast to EncryptedPacket. Class not found");
		}

		return encData;
	}

	private boolean sendFileToServer(String serverAlias, EncryptedPacket packet, ObjectOutputStream outputStream,
			ObjectInputStream inputStream) {

		try {
			outputStream.writeObject(packet);

			// TODO receive ACK
			EncryptedPacket encResponse = (EncryptedPacket) inputStream.readObject();

			Packet response = tryReadMessage(encResponse);

			if (response == null) {
				System.out.println("Error receiving ACK from server. (file backup)");
				return false;
			}

			if (!response.get_packet_type().equals(PacketType.ACK)) {
				return false;
			}

			return true;

		} catch (IOException e) {
			System.out.println("Error with streams");
			return false;
		} catch (ClassNotFoundException e) {
			System.out.println("Could not cast to EncryptedPacket. Class not found");
			return false;
		}
	}

	/**
	 * Tries to send a backup conversation file to a set of servers. This method
	 * might be a point of concurrency
	 * 
	 * @param msg   the message to update the files with
	 * @param alias the name of the conversation file
	 */
	public synchronized void trySendToServers(String msg, String alias) {
		List<ObjectOutputStream> outputStreams = new ArrayList<ObjectOutputStream>();
		List<ObjectInputStream> inputStreams = new ArrayList<ObjectInputStream>();
		
		// TODO: if all servers are down switch to a local backup
		SSLSocket[] servers = connectToBackupServer();
		
		// check if any server has the file
		// if one server has the file stop loop
		// retrieve file
		// decrypt
		// add message
		// encrypt
		// send to all servers

		String encData = null;
		for (int i = 0; i < servers.length; i++) {
			SSLSocket currentServer = servers[i];
			
			if (currentServer == null) {
				continue; // server connection was not established (might be down for maintenance)
			}

			try {
				String serverAlias = serverAliases.get(i);
				ObjectOutputStream out = new ObjectOutputStream(currentServer.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(currentServer.getInputStream());

				//System.out.println("Saving server socket streams");
				outputStreams.add(out);
				inputStreams.add(in);

				if ((encData = retrieveFile(serverAlias, alias + ".conversation", out, in)) != null) {
					break;
				}

			} catch (IOException e) {
				System.out.println("Error with streams");
				e.printStackTrace();
			}
		}

		File conversationFile;

		if (encData == null) {
			// criar file
			//System.out.println("Creating new .conversation file");
			conversationFile = new File("conversations/"+alias + ".conversation");
		} else {
			// decrypt file
			//System.out.println("Decrypting .conversation file");
			try {
				conversationFile = HybridEncryption.decryptFile(alias + ".conversation",encData,
						(PrivateKey) keyStore.getKey(name, password.toCharArray()));
			} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
				e.printStackTrace();
				return;
			}
		}

		//System.out.println("Writing new message...");
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(conversationFile, true))) {
			// add message
			writer.write(msg);
			writer.newLine();
		} catch (IOException e) {
			System.err.println("An error occurred while writing the message to the file: " + e.getMessage());
		}

		//System.out.println("Encrypting updated file...");
		String encFile = null;
		try {
			encFile = HybridEncryption.encryptFile(conversationFile, trustStore.getCertificate(name).getPublicKey());
		} catch (KeyStoreException e) {
			System.out.println("Could not retrieve public key");
			e.printStackTrace();
		}

		if (encFile == null) {
			System.out.println("Error trying to encrypt file for backup servers");
			return;
		}

		//System.out.println("Sending file to backup servers...");
		int successfullBackups = 0;
		for (int i = 0; i < servers.length; i++) {
			if (servers[i] == null) {
				continue;
			}

			String serverAlias = serverAliases.get(i);
			// send file
			//System.out.println("Sending file to server (" + i + ") " + serverAlias);
			ObjectOutputStream out = outputStreams.get(i);
			ObjectInputStream in = inputStreams.get(i);
			EncryptedPacket encFilePacket = encryptPacket(serverAlias, alias + ".conversation " + encFile,
					PacketType.BACKUP);

			if (sendFileToServer(serverAlias, encFilePacket, out, in)) {
				successfullBackups++;
				//System.out.println("File backup successful. Servers[" + successfullBackups + "/3]");
			}

			try {
				servers[i].close();
			} catch (IOException e) {
				System.out.println("Error closing connection to backup server "+  i);
			}
		}
	}

	public void try_send_message(Scanner sc, String alias) {
		boolean is_writing = true;
		System.out.printf("----------- Messaging %s -----------\n", alias);

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

				Packet ack;

				if ((ack = send_message(encPacket, in, out)) == null) {
					is_writing = false;
					System.out.println("Error sending message or message was blank/empty");
					continue;
				}
				
				
				trySendToServers(name + ":" + msg, alias);
				//local backup
				//MessageLogger.write_message_log(name + ": " + msg, ack.get_sender() +".conversation");
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

	public EncryptedPacket encryptPacket(String alias, Packet packet) {

		try {
//			System.out.println("Retrieving pk from " + alias);
			PublicKey pk = trustStore.getCertificate(alias).getPublicKey();

			if (pk == null) {
				System.out.println("Error retrieving assymetric keys");
				System.exit(-1);
			}

//			System.out.println("Encrypting packet");
			EncryptedPacket encPacket = HybridEncryption.encryptPacket(packet, pk);

			if(encPacket == null) {
				return null;
			}
			
//			System.out.println("Packet encrypted");
			
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
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Open a conversation with a peer if a conversation does not exist it creates a
	 * new one. This method is also responsible for connecting to the peer?????
	 * 
	 * @param conversation_id the id of the conversation to open
	 * @return 0 if could open the conversation -1 in case of error
	 */
	public int open_conversation(int conversation_id) {
		if (conversations.length == 0 || conversation_id >= conversations.length || conversation_id < 0) {
			System.out.println("The conversation you are trying to open does not exist");
			return -1;
		}

		String conversation = MessageLogger.read_message_log(conversations[conversation_id] + ".conversation");

		if (conversation == null) {
			return -1;
		}

		System.out.println(conversation);

		return 0;
	}

	private void checkForConversations() {
		HashSet<String> filesInServers = new HashSet<String>();
		List<ObjectOutputStream> outputStreams = new ArrayList<ObjectOutputStream>();
		List<ObjectInputStream> inputStreams = new ArrayList<ObjectInputStream>();
		
		SSLSocket[] servers = connectToBackupServer();
		
		// check if any server has the file
		// retrieve filename
		// decrypt
		// add name
		//retrieve all files 

//		String encData = null;
//		for (int i = 0; i < servers.length; i++) {
//			SSLSocket currentServer = servers[i];
//			
//			if (currentServer == null) {
//				continue; // server connection was not established (might be down for maintenance)
//			}
//
//			try {
//				String serverAlias = serverAliases.get(i);
//				ObjectOutputStream out = new ObjectOutputStream(currentServer.getOutputStream());
//				ObjectInputStream in = new ObjectInputStream(currentServer.getInputStream());
//
//				//System.out.println("Saving server socket streams");
//				outputStreams.add(out);
//				inputStreams.add(in);
//
//				if ((encData = retrieveFile(serverAlias, alias + ".conversation", out, in)) != null) {
//					break;
//				}
//
//			} catch (IOException e) {
//				System.out.println("Error with streams");
//				e.printStackTrace();
//			}
//		}

		
	}
	
	public void list_conversations() {
		//TODO: change get conversations to retrieve any missing files from the backup servers
		conversations = MessageLogger.getLocalConversations();
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