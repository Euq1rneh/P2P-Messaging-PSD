package client.peer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.nio.ByteBuffer;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import client.peer.crypto.HybridEncryption;
import client.peer.crypto.ShamirScheme;
import client.peer.messages.MessageLogger;
import client.peer.network.ConnectionManager;
import client.peer.threads.ConnectionAcceptorThread;
import common.EncryptedPacket;
import common.Packet;
import common.PacketType;

/**
 * The Peer class represents a network peer capable of establishing connections, exchanging messages, and
 * performing cryptographic operations to ensure secure communication and storage.
 */
public class Peer {

	private static final int SHARE_THRESHOLD = 2; // ALLOWS FOR AT LEAST ONE TO FAIL (SERVER)
	private static final int NUM_SHARES = 3; // EACH SERVER HOLDS 1

	private static final String HMAC_ALG = "HmacSHA1";
	private static final String CIPHER_ALG = "AES";

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
	private HashMap<Integer, String> serverAdresses = new HashMap<Integer, String>();
	private KeyManager[] keyManagers;
	private TrustManager[] trustManagers;

	private ShamirScheme scheme;

	private SecretKey masterKey;
	private Map<String, Integer> counters = Collections.synchronizedMap(new HashMap<>(100));

	private SecretKeySpec sk;
	private IvParameterSpec iv;
	private Mac hmac;
	private Cipher aes;

	/**
     * Constructs a Peer instance with the specified parameters.
     *
     * @param name       the name of the peer
     * @param in_port    the port to listen on for incoming connections
     * @param keyStore   the KeyStore instance for storing cryptographic keys
     * @param trustStore the TrustStore instance for validating connections
     * @param password   the password for accessing the key stores
     */
	public Peer(String name, int in_port, KeyStore keyStore, KeyStore trustStore, String password) {
		this.name = name;
		this.in_port = in_port;
		this.keyStore = keyStore;
		this.trustStore = trustStore;
		this.password = password;

		serverAliases.put(0, "amazonServer");
		serverAliases.put(1, "oracleServer");
		serverAliases.put(2, "googleServer");

		serverAdresses.put(0, "127.0.0.1:1111");
		serverAdresses.put(1, "127.0.0.1:2222");
		serverAdresses.put(2, "127.0.0.1:3333");

		scheme = new ShamirScheme(new SecureRandom(), NUM_SHARES, SHARE_THRESHOLD);
		try {
			hmac = Mac.getInstance(HMAC_ALG);
			aes = Cipher.getInstance("AES/CBC/PKCS5Padding");
		} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
     * Initializes the secret key and initialization vector (IV) used for SE
     */
	private void initSKIV() {
		if (sk == null || iv == null) {
			try {
				retrieveMasterKey();
			} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException
					| IOException e) {
				System.out.println("Error while trying to retrieve master key");
				return;
			}

			byte[] sk_bytes = new byte[20];
			byte[] iv_bytes = new byte[16];
			File paramFile = new File("se-params.dat");

			if (paramFile.exists()) {
				// read and decode params
				loadSEParams();
//				System.out.println("SKIV loaded");
			} else {
				// create and save params
				SecureRandom rnd = new SecureRandom();
				rnd.nextBytes(sk_bytes);
				rnd.nextBytes(iv_bytes);

				saveSEParams(sk_bytes, iv_bytes);
//				System.out.println("New SKIV");
				sk = new SecretKeySpec(sk_bytes, HMAC_ALG);
				iv = new IvParameterSpec(iv_bytes); // should change for every entry
			}
			
//			System.out.println("Loaded SK: " + Base64.getEncoder().encodeToString(sk.getEncoded()));
			return;
		}
//		System.out.println("SKIV already loaded");
	}
	
	/**
     * Saves the current state of counters to a file with encryption.
     *
     * @throws Exception if an error occurs during file writing or encryption
     */
    public void saveCounters() throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("se-counters.dat"))) {
            // Generate a new IV for this session
            byte[] ivBytes = new byte[16];
            new java.security.SecureRandom().nextBytes(ivBytes);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            // Write the IV as Base64 in the first line
            String ivBase64 = Base64.getEncoder().encodeToString(ivBytes);
            writer.write(ivBase64);
            writer.newLine();

            // Initialize the cipher for encryption
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, iv);

            for (Map.Entry<String, Integer> entry : counters.entrySet()) {
                // Serialize the key-value pair as "key=value"
                String plainText = entry.getKey() + "=" + entry.getValue();

                // Encrypt the data
                byte[] encryptedData = cipher.doFinal(plainText.getBytes());

                // Encode to Base64
                String base64Encoded = Base64.getEncoder().encodeToString(encryptedData);
                writer.write(base64Encoded);
                writer.newLine();
            }
        }
    }
    
    /**
     * Loads the state of counters from a file and decodes the contents.
     *
     * @throws Exception if an error occurs during file reading or decoding
     */
    public void loadCounters() throws Exception {
        counters.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader("se-counters.dat"))) {
            // Read the IV from the first line
            String ivBase64 = reader.readLine();
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            // Initialize the cipher for decryption
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, masterKey, iv);

            String line;
            while ((line = reader.readLine()) != null) {
                // Decode Base64
                byte[] encryptedData = Base64.getDecoder().decode(line);

                // Decrypt the data
                String decryptedText = new String(cipher.doFinal(encryptedData));

                // Parse the key-value pair
                String[] parts = decryptedText.split("=", 2);
                if (parts.length == 2) {
                    counters.put(parts[0], Integer.parseInt(parts[1]));
                }
            }
        }
    }
	
    /**
     * Connects to another peer using the specified IP address and port.
     *
     * @param address the IP address of the peer
     * @param port    the port of the peer
     * @return 0 if the connection is successful, -1 otherwise
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
		SSLSocket[] servers = new SSLSocket[serverAdresses.size()];

		for (int i = 0; i < serverAdresses.size(); i++) {
			String[] connectionArgs = serverAdresses.get(i).split(":");

			servers[i] = ConnectionManager.try_connect_to_peer(keyStore, trustStore, keyManagers, trustManagers,
					connectionArgs[0], Integer.parseInt(connectionArgs[1]));

			if (servers[i] == null) {
//				System.out.println(
//						"Could not connect to server with address " + connectionArgs[0] + ":" + connectionArgs[1]);
			}
		}

		return servers;
	}

	private String joinShares(Map<Integer, byte[]> parts) {
		byte[] byteSecret = scheme.join(parts);

		return new String(byteSecret);
	}

	private Map<Integer, byte[]> splitSecret(String secret) {
//		System.out.println("Secret=" + secret);
		return scheme.split(secret.getBytes());
	}

	 /**
     * Attempts to retrieve a file share from a backup server.
     *
     * @param serverAlias  the server alias for key retrieval
     * @param filename     the name of the file to retrieve
     * @param outputStream the output stream of the socket connection to the server
     * @param inputStream  the input stream of the socket connection to the server
     * @return the file contents if found, null otherwise
     */
	private String retrieveShare(String serverAlias, String filename, ObjectOutputStream outputStream,
			ObjectInputStream inputStream) {
		Packet p = new Packet(name, filename, PacketType.RET_FILE);

		String encData = null;
//		System.out.println("Searching for existing file in server " + serverAlias);

		EncryptedPacket encRequest = encryptPacket(serverAlias, p);

		try {

//			System.out.println("Sending share request");
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

			if (response == null) {
				System.out.println("Error while trying to read response from server");
				return null;
			}

			if ((encData = response.get_data()) != null) {
//				System.out.println("Server has share");
//				System.out.println("\n\nRetrived share = " + encData);
				return encData;
			}

//			System.out.println("Server did not have backup file. Asking other servers...");

		} catch (IOException e) {
			System.out.println("Error with streams");
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.out.println("Could not cast to EncryptedPacket. Class not found");
		}

		return null;
	}

	private boolean sendFileToServer(String serverAlias, EncryptedPacket packet, ObjectOutputStream outputStream,
			ObjectInputStream inputStream) {

		try {
			outputStream.writeObject(packet);

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

		String filename = alias + ".conversation";
		HashMap<Integer, byte[]> parts = new HashMap<Integer, byte[]>();
		int downServers = 0;

		// Searchable encryption
		String[] terms = msg.substring(msg.indexOf(":") + 1).split(" ");
		for (String word : terms) {
			if (word.equals(":") || word.equals(" ")) {
				continue;
			}

			try {
//				System.out.println("Updating search with term: " + word);
				updateSearchTerms(word, filename);
			} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
					| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// loop to retrive available shares
		for (int i = 0; i < servers.length; i++) {
			SSLSocket currentServer = servers[i];

			if (currentServer == null) {
				downServers++;
				outputStreams.add(null);
				inputStreams.add(null);
				continue; // server connection was not established (might be down for maintenance)
			}

			try {
				String serverAlias = serverAliases.get(i);
				ObjectOutputStream out = new ObjectOutputStream(currentServer.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(currentServer.getInputStream());

				outputStreams.add(out);
				inputStreams.add(in);

//				System.out.println("Trying to retrieve share");
				// Secret Sharing
				String base64Share = retrieveShare(serverAlias, filename, out, in);

//				System.out.printf("Retrieved share (%s)= %s",serverAlias,  base64Share);
				// add share
				if (base64Share != null) {
//					System.out.println("Share retrieved successfully decoding...");
//					System.out.printf("Server %d b64share= " + base64Share + "\n", serverAlias);
					byte[] share = Base64.getDecoder().decode(base64Share);
//					System.out.println("Adding share...");
					parts.put(i + 1, share);
				}

			} catch (IOException e) {
				System.out.println("Error with streams");
				e.printStackTrace();
			}
		}

		// TODO: maybe keep a local copy of search terms to send to the servers
		if (downServers > SHARE_THRESHOLD) {
//			System.out.println("Using local backup");
			MessageLogger.writeMessageLog(msg, filename);
			return;
		}

		File conversationFile = null;
		boolean cannotRebuildFile = parts.size() < SHARE_THRESHOLD;

		// doesnt have enough shares
		if (cannotRebuildFile) {
			conversationFile = new File("conversations/" + alias + ".conversation");
		} else {
			String encData = joinShares(parts);
//			System.out.println("Rebuilt file...");
			// decrypt file
			try {
				conversationFile = HybridEncryption.decryptFile(alias + ".conversation", encData,
						(PrivateKey) keyStore.getKey(name, password.toCharArray()));
			} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
				e.printStackTrace();
				return;
			}
		}

//		System.out.println("Writing new message...");
		MessageLogger.writeMessageLog(msg, filename);

//		System.out.println("Encrypting updated file...");
		String secret = null;
		try {
			// returns wrapped key + @ + file bytes
			secret = HybridEncryption.encryptFile(conversationFile, trustStore.getCertificate(name).getPublicKey());
		} catch (KeyStoreException e) {
			System.out.println("Could not retrieve public key");
			e.printStackTrace();
		}

		if (secret == null) {
			System.out.println("Error trying to encrypt file for backup servers");
			return;
		}

		Map<Integer, byte[]> shares = splitSecret(secret);

//		System.out.println(joinShares(shares));
//		System.out.println("Sending file to backup servers...");
		int successfullBackups = 0;
		for (int i = 0; i < servers.length; i++) {
			if (servers[i] == null) {
				continue;
			}

			String serverAlias = serverAliases.get(i);
			// send file
//			System.out.println("Sending share to server (" + i + ") " + serverAlias);
			ObjectOutputStream out = outputStreams.get(i);
			ObjectInputStream in = inputStreams.get(i);

			String encData = Base64.getEncoder().encodeToString(shares.get(i + 1));

			EncryptedPacket encFilePacket = encryptPacket(serverAlias, alias + ".conversation " + encData,
					PacketType.BACKUP);

			if (sendFileToServer(serverAlias, encFilePacket, out, in)) {
				successfullBackups++;
			}

			try {
				servers[i].close();
			} catch (IOException e) {
				System.out.println("Error closing connection to backup server " + i);
			}
		}
//		System.out.println("File backup successful. Servers[" + successfullBackups + "/3]");
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
				// local backup
				// MessageLogger.write_message_log(name + ": " + msg, ack.get_sender()
				// +".conversation");
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

	/**
     * Encrypts a packet with the specified alias, message, and packet type.
     *
     * @param alias the alias of the recipient
     * @param msg   the message to encrypt
     * @param type  the type of the packet
     * @return the encrypted packet
     */
	public EncryptedPacket encryptPacket(String alias, String msg, PacketType type) {
//		System.out.println("Encrypting packet for " + alias);
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
     * Encrypts a packet with the specified alias and packet data.
     *
     * @param alias  the alias of the recipient
     * @param packet the packet to encrypt
     * @return the encrypted packet
     */
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

			if (encPacket == null) {
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
     * Sends a message to the specified peer.
     *
     * @param encPacket the encrypted packet to send
     * @param in        the input stream to the peer
     * @param out       the output stream to the peer
     * @return an acknowledgment packet if successful, null otherwise
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
     * Attempts to read an encrypted message from a peer.
     *
     * @param message the encrypted message
     * @return the decoded packet
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

		String conversation = MessageLogger.readMessageLog(conversations[conversation_id] + ".conversation");

		if (conversation == null) {
			return -1;
		}

		System.out.println(conversation);

		return 0;
	}

	private void getAvailableFiles(HashSet<String> filesInServers, HashMap<String, List<String>> filesPerServer,
			List<ObjectOutputStream> outputStreams, List<ObjectInputStream> inputStreams, SSLSocket[] servers) {

		for (int i = 0; i < servers.length; i++) {
			SSLSocket currentServer = servers[i];
			String serverAlias = serverAliases.get(i);

//			System.out.println("Current server -> " + serverAlias);
			if (currentServer == null) {
//				System.out.println("could not send message");
				outputStreams.add(null);
				inputStreams.add(null);
				continue; // server connection was not established (might be down for maintenance)
			}

			try {

				ObjectOutputStream out = new ObjectOutputStream(currentServer.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(currentServer.getInputStream());

				outputStreams.add(out);
				inputStreams.add(in);
				filesPerServer.put(serverAlias, new ArrayList<String>());

				// send files request
				EncryptedPacket ep = encryptPacket(serverAlias, "", PacketType.AVAILABLE_FILES);
				if (ep == null) {
					System.out.println("Error while trying to retrieve " + serverAlias + " key");
					continue;
				}
//				System.out.println("Sending available file request to server");
				out.writeObject(ep);

				EncryptedPacket encResponse = (EncryptedPacket) in.readObject();
//				System.out.println("Reading response from server");
				Packet p = tryReadMessage(encResponse);

				if (p == null) {
//					System.out.println("Could not read server response");
					continue;
				}

				if (p.get_data().equals("")) {
					continue;// no files available
				}

//				System.out.println("Adding file names");
				String[] files = p.get_data().split(" ");
				List<String> fileList = Arrays.asList(files);

				List<String> serverAvailableFiles = filesPerServer.get(serverAlias);
				serverAvailableFiles.addAll(fileList);
				filesInServers.addAll(fileList);

//				System.out.println("Current available files: " + filesInServers.toString());

			} catch (IOException e) {
				System.out.println("Error with streams");
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	private void retrieveAvailableFiles(HashMap<String, List<String>> filesPerServer,
			List<ObjectOutputStream> outputStreams, List<ObjectInputStream> inputStreams, SSLSocket[] servers) {

		HashMap<String, HashMap<Integer, byte[]>> filesShares = new HashMap<String, HashMap<Integer, byte[]>>();

		// iterate through all servers
		for (int i = 0; i < servers.length; i++) {
			if (servers[i] == null) {
				outputStreams.add(null);
				inputStreams.add(null);
				continue;
			}

			ObjectOutputStream out = outputStreams.get(i);
			ObjectInputStream in = inputStreams.get(i);
			String serverAlias = serverAliases.get(i);

			List<String> filenames = filesPerServer.get(serverAlias);

			// iterate through all files available in the server
			for (String filename : filenames) {
				String encShare = retrieveShare(serverAlias, filename, out, in);

				if (encShare == null) {
					continue;
				}

//				System.out.println("New share retrieved");

				// get each share
				byte[] shareB64 = Base64.getDecoder().decode(encShare.getBytes());

				if (filesShares.containsKey(filename)) {
					HashMap<Integer, byte[]> shares = filesShares.get(filename);

					if (shares == null) {
						System.out.println("Error");
						return;
					}

					shares.put(i + 1, shareB64);
				} else {
					HashMap<Integer, byte[]> shares = new HashMap<Integer, byte[]>();
					shares.put(i + 1, shareB64);
					filesShares.put(filename, shares);
				}
			}
		}

		for (Map.Entry<String, HashMap<Integer, byte[]>> entry : filesShares.entrySet()) {
			String filename = entry.getKey();
			HashMap<Integer, byte[]> shares = entry.getValue();
			File rebuiltFile = null;
//			System.out.println("Trying to rebuild " + filename);
			if (couldRebuildFile(filename, shares, rebuiltFile)) {
//				System.out.println("File rebuilt with success");	
			}
			else {
//				System.out.println("Could not rebuild file");				
			}

		}

		System.out.flush();
	}

	private boolean couldRebuildFile(String filename, HashMap<Integer, byte[]> shares, File returnFile) {

		// Does not have enough shares
		if (shares.size() < SHARE_THRESHOLD) {
			return false;
		}

		String encData = joinShares(shares);

		try {
			returnFile = HybridEncryption.decryptFile(filename, encData,
					(PrivateKey) keyStore.getKey(name, password.toCharArray()));

			if (returnFile != null) {
				// last operation
				return true;
			}
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		}

		return false;
	}

	private void checkForConversationsInServers() {
		HashSet<String> filesInServers = new HashSet<String>();
		HashMap<String, List<String>> filesPerServer = new HashMap<String, List<String>>();
		List<ObjectOutputStream> outputStreams = new ArrayList<ObjectOutputStream>();
		List<ObjectInputStream> inputStreams = new ArrayList<ObjectInputStream>();

		SSLSocket[] servers = connectToBackupServer();

		getAvailableFiles(filesInServers, filesPerServer, outputStreams, inputStreams, servers);
		retrieveAvailableFiles(filesPerServer, outputStreams, inputStreams, servers);

		for (int i = 0; i < servers.length; i++) {
			try {
				if (servers[i] != null) {
					servers[i].close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void list_conversations() {
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

	private void retrieveMasterKey() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, IOException {
		masterKey = (SecretKey) keyStore.getKey("masterKey", password.toCharArray());
		if (masterKey == null) {
			System.out.println("Key does not exist. Creating new master key...");
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(256);
			masterKey = keyGen.generateKey();

			// save key to keystore
			System.out.println("Saving master key...");
			KeyStore.SecretKeyEntry keyEntry = new KeyStore.SecretKeyEntry(masterKey);
			KeyStore.ProtectionParameter protectionParam = new KeyStore.PasswordProtection(password.toCharArray());
			keyStore.setEntry("masterKey", keyEntry, protectionParam);

			try (FileOutputStream fos = new FileOutputStream("keystore/" + name + "-keystore.jceks")) {
				keyStore.store(fos, password.toCharArray()); // Save the keystore
			}
		}
	}

	private void saveSEParams(byte[] sk, byte[] iv) {
		try {
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, masterKey);

			// Encrypt sk and iv
			byte[] encryptedSk = cipher.doFinal(sk);
			byte[] encryptedIv = cipher.doFinal(iv);

			// Combine encrypted sk and iv (you can separate them in the file if needed)
			byte[] encryptedData = new byte[encryptedSk.length + encryptedIv.length];
			System.arraycopy(encryptedSk, 0, encryptedData, 0, encryptedSk.length);
			System.arraycopy(encryptedIv, 0, encryptedData, encryptedSk.length, encryptedIv.length);

			// Convert encrypted data to base64
			String base64Encoded = Base64.getEncoder().encodeToString(encryptedData);

			// Save the base64 encoded data to a file
			try (FileOutputStream fileOutputStream = new FileOutputStream("se-params.dat")) {
				fileOutputStream.write(base64Encoded.getBytes());
				System.out.println("se-params.dat file has been saved with encrypted parameters.");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void loadSEParams() {
		try {
			// Read the base64 encoded data from the file
			FileInputStream fileInputStream = new FileInputStream("se-params.dat");
			byte[] fileData = new byte[fileInputStream.available()];
			fileInputStream.read(fileData);
			fileInputStream.close();

			// Decode the base64 data
			byte[] decodedData = Base64.getDecoder().decode(fileData);

			// Calculate the lengths dynamically
			int skLength = decodedData.length / 2; // Assuming sk and iv are equal lengths, or adjust if not
			int ivLength = decodedData.length - skLength;

			byte[] encryptedSk = new byte[skLength];
			byte[] encryptedIv = new byte[ivLength];
			System.arraycopy(decodedData, 0, encryptedSk, 0, skLength);
			System.arraycopy(decodedData, skLength, encryptedIv, 0, ivLength);

			Cipher cipher = Cipher.getInstance("AES");

			cipher.init(Cipher.DECRYPT_MODE, masterKey);

			// Decrypt the sk and iv
			byte[] sk_bytes = cipher.doFinal(encryptedSk);
			byte[] iv_bytes = cipher.doFinal(encryptedIv);

			sk = new SecretKeySpec(sk_bytes, HMAC_ALG);
			iv = new IvParameterSpec(iv_bytes); // should change for every entry
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void searchInConversations(String keyword) throws NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		SSLSocket[] servers = connectToBackupServer();

		if (backupServersDown(servers)) {
			System.out.println("Searching locally...");
			searchLocal(keyword);
		} else {
			// Generate k1 and k2 from keyword
		    hmac.init(sk);
		    SecretKeySpec k1 = new SecretKeySpec(hmac.doFinal((keyword + "1").getBytes()), HMAC_ALG);
			SecretKeySpec k2 = new SecretKeySpec(hmac.doFinal((keyword + "2").getBytes()), 0, 16, CIPHER_ALG);

		    String b64K1 = Base64.getEncoder().encodeToString(k1.getEncoded());

		    String searchResult = searchTermToServers(servers, b64K1);
		    //close connections to server
		    for (int i = 0; i < servers.length; i++) {
				try {
					if (servers[i] != null) {
						servers[i].close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		    
		    if(searchResult == null) {
		    	System.out.println("No search result found");
		    	return;
		    }
		    String[] b64Results = searchResult.split("@");
		    List<byte[]> encryptedResults = new ArrayList<byte[]>();
		    for (String string : b64Results) {
				encryptedResults.add(Base64.getDecoder().decode(string));
			}
		    
		    // Decrypt the results using k2
		    Set<String> results = new HashSet<String>();
		    aes.init(Cipher.DECRYPT_MODE, k2, iv);
		    for (byte[] encryptedDocName : encryptedResults) {
		        byte[] docNameBytes = aes.doFinal(encryptedDocName);
		        results.add(new String(docNameBytes)); // Convert decrypted bytes back into a string
		    }
		    System.out.println("Search result(s) for " + keyword+" returned the following files:");
		    for (String string : results) {
				System.out.println("    - "+string);
			}
		}
	}

	private void updateSearchTerms(String keyword, String filename)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {

		hmac.init(sk);
		SecretKeySpec k1 = new SecretKeySpec(hmac.doFinal((keyword + "1").getBytes()), HMAC_ALG);
		SecretKeySpec k2 = new SecretKeySpec(hmac.doFinal((keyword + "2").getBytes()), 0, 16, CIPHER_ALG);

		int c = counters.getOrDefault(keyword, 0);

		hmac.init(k1);
		byte[] l = hmac.doFinal(ByteBuffer.allocate(4).putInt(c).array());

		aes.init(Cipher.ENCRYPT_MODE, k2, iv);
		byte[] d = aes.doFinal(filename.getBytes());

		// Send l and d to the server to update the index
		String term = Base64.getEncoder().encodeToString(l) + "@" + Base64.getEncoder().encodeToString(d);
//		System.out.println("New Search term entry= " + term);
		sendTermToServers(term);
		// Increment counter c and update it in counters
		counters.put(keyword, ++c);
		
//		System.out.println("COUNTERS=" + counters.toString());
		
		try {
			saveCounters();
		} catch (Exception e) {
			System.out.println("Error saving counters");
		}
	}

	/**
	 * Sends a new search term to the servers
	 * 
	 * @param term the new term
	 * @return the search result null otherwise
	 */
	private String searchTermToServers(SSLSocket[]servers, String term) {

		if (backupServersDown(servers)) {
			return null;
		}

		for (int i = 0; i < servers.length; i++) {

			if (servers[i] == null) {
				continue;
			}
			try {
				ObjectOutputStream out = new ObjectOutputStream(servers[i].getOutputStream());
				ObjectInputStream in = new ObjectInputStream(servers[i].getInputStream());

				String serverAlias = serverAliases.get(i);

				EncryptedPacket ep = encryptPacket(serverAlias, term, PacketType.SEARCH);
				out.writeObject(ep);

				EncryptedPacket er = (EncryptedPacket) in.readObject();

				Packet p = tryReadMessage(er);

				if(p == null) {
					return null;
				}
				
				return p.get_data();
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	/**
	 * Sends a new search term to the servers
	 * 
	 * @param term the new term
	 * @return 0 if successful, -2 if all servers are down, -1 in case of error
	 */
	private int sendTermToServers(String term) {
		SSLSocket[] servers = connectToBackupServer();

		if (backupServersDown(servers)) {
			return -2;
		}

		for (int i = 0; i < servers.length; i++) {

			if (servers[i] == null) {
				continue;
			}
			try {
				ObjectOutputStream out = new ObjectOutputStream(servers[i].getOutputStream());
				ObjectInputStream in = new ObjectInputStream(servers[i].getInputStream());

				String serverAlias = serverAliases.get(i);

				EncryptedPacket ep = encryptPacket(serverAlias, term, PacketType.ADD_KEYWORD);
				out.writeObject(ep);

				EncryptedPacket er = (EncryptedPacket) in.readObject();

				Packet p = tryReadMessage(er);
				
				servers[i].close();
				if (!p.get_packet_type().equals(PacketType.ACK)) {
					return -1;
				}
			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
				return -1;
			}
		}

		return 0;
	}

	private boolean backupServersDown(SSLSocket[] backups) {
		return backups == null || Arrays.stream(backups).allMatch(e -> e == null); // might be unnecessary considering
																					// it's 1 line
	}

	private void searchLocal(String keywords) {
		// i imagine this has a bit of repeated code
		// also not sure if this conversation related function belongs in Peer or
		// somewhere else
		Boolean found = false;
		for (String conversation : conversations) {
			String contents = MessageLogger.readMessageLog(conversation + ".conversation");

			int occurrences = countOccurrences(contents, keywords);
			if (occurrences == 0) {
				continue;
			}
			found = true;
			System.out.println("The keyword(s) provided were found " + occurrences + " times in the conversation with "
					+ conversation + ".\n");
		}

		if (!found) {
			System.out.println("The keywords '" + keywords + "' were not found.");
		}
	}

	private int countOccurrences(String conversation, String keywords) {
		conversation = conversation.toLowerCase(); // this to make it case insensitive
		keywords = keywords.toLowerCase();

		String[] lines = conversation.split("\n");
		int count = 0;

		for (String line : lines) {
			String trimmedLine = line.replaceAll("^[^:]+:", "").trim();

			int index = trimmedLine.indexOf(keywords);
			while (index >= 0) {
				count++;
				index = trimmedLine.indexOf(keywords, index + keywords.length());
			}
		}

		return count;
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
     * Starts the peer, initializing necessary components and accepting connections.
     *
     * @param running   a flag indicating whether the peer should continue running
     * @param keyStore  the KeyStore for cryptographic operations
     * @param password  the password for the KeyStore
     */
	public void start(boolean running, KeyStore keyStore, String password) {

		MessageLogger.buildConversationDir();
		initSKIV();
		try {
			loadCounters();
//			System.out.println("COUNTERS="+ counters.toString());
		} catch (Exception e) {
			System.out.println("Error loading SE counters");
		}
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
		// try retrieve any available files in the servers on startup
		checkForConversationsInServers();
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