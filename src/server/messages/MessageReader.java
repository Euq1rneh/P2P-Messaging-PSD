package server.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.InvalidKeyException;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import javax.net.ssl.SSLSocket;

import common.ByteArray;
import common.EncryptedPacket;
import common.Packet;
import common.PacketType;
import server.crypto.Encryption;
import server.server.Main;

public class MessageReader implements Runnable {
	private final SSLSocket socket;

	boolean running_status;
	private ServerMaps serverMaps = null;

	public MessageReader(SSLSocket socket, boolean running_status) {
		this.socket = socket;
		this.running_status = running_status;
	}

	@Override
	public void run() {
		try {
			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

			while (running_status) {

				EncryptedPacket packet = (EncryptedPacket) in.readObject();
				Packet p = Encryption.tryReadMessage(packet);

				if (p == null) {
					EncryptedPacket errorPacket = new EncryptedPacket(null, null, null);
					out.writeObject(errorPacket);
					continue;
				}

				Packet response;
				EncryptedPacket encryptedResponse = new EncryptedPacket(null, null, null);

				response = processMessage(p);
				
				//error while processing message
				if (response == null) {
					out.writeObject(encryptedResponse);
					continue;
				}
				
				encryptedResponse = Encryption.encryptPacket(response, p.get_sender());

				out.writeObject(encryptedResponse);
			}
		} catch (IOException e) {
			System.out.println("Client connection closed");
		} catch (ClassNotFoundException e) {
			System.out.println("Error: Packet class not found - " + e.getMessage());
		}
	}

	private Packet processMessage(Packet p) {
		Packet response = null;

		if(serverMaps == null) {
			serverMaps = new ServerMaps(Main.getSearchMap(p.get_sender()));
		}
		
		System.out.println("Processing request from client " + p.get_sender());
		switch (p.get_packet_type()) {
		case PacketType.RET_FILE:
			System.out.println("Processing file retrieval request");
			String fileContents = ServerFiles.retrieve(p.get_sender(), p.get_data());
			
			if(fileContents != null) {
				response = new Packet("server", fileContents, PacketType.RET_FILE);				
			}
			
			break;
		case PacketType.BACKUP:
			System.out.println("Processing file backup request");
			String[] args = p.get_data().split(" ");
			String filename = args[0];
			String contents = args[1];
			
//			System.out.println("File backup contents=" + contents);
			
			if(ServerFiles.backup(p.get_sender(), filename, contents)) {
				response = new Packet("server", "", PacketType.ACK);
			}
			
			break;
		case PacketType.AVAILABLE_FILES:
			System.out.println("Processing available files request");
			String user = p.get_sender();
			String fileNames = ServerFiles.getAvailableFiles(user);
//			System.out.println("Available files: " + fileNames);
			
			
			response = new Packet("server", fileNames, PacketType.AVAILABLE_FILES);
			
			break;
		case PacketType.SEARCH:
			System.out.println("Processing search request");
			byte[] searchTerm = Base64.getDecoder().decode(p.get_data());
			System.out.println("Search term= " + p.get_data());
			try {
				List<byte[]> results = serverMaps.search(searchTerm);
				System.out.println("Number of results found =" + results.size());
				if(results != null && !results.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					
					for (byte[] byteArray : results) {
						sb.append(Base64.getEncoder().encodeToString(byteArray)+ "@");
					}
					//remove the last @
					sb.deleteCharAt(sb.length()-1);
					System.out.println("Compacting results...");
					String compactedResults = sb.toString();
					System.out.println("Compacted: " + compactedResults);
					
					response = new Packet("server", compactedResults, PacketType.SEARCH);
				}
				
			} catch (InvalidKeyException e) {
				System.out.println("Error while searching for term");
				e.printStackTrace();
			}	
			
			break;
		case PacketType.ADD_KEYWORD:
			System.out.println("Processing adding keyword");
			String[] data = p.get_data().split("@");
			
			if(data.length != 2)
				break;
			
			System.out.println("New search entry= "+ p.get_data());
			byte[] labelByte = Base64.getDecoder().decode(data[0]);
			byte[] dataByte = Base64.getDecoder().decode(data[1]);
			
			ByteArray l = new ByteArray(labelByte);
			ByteArray d = new ByteArray(dataByte);
			
			System.out.println("label= " + l.getArr().toString());
			System.out.println("data= " + d.getArr().toString());
			
			serverMaps.update(l, d);
			
			response = new Packet("server", "", PacketType.ACK);
			break;
		default:
			System.out.println("Could not process packet. Did not recognize type " + p.get_packet_type().toString());
			break;
		}

		return response;
	}
}
