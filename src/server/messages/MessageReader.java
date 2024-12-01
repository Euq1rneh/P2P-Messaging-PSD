package server.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.net.ssl.SSLSocket;

import common.EncryptedPacket;
import common.Packet;
import common.PacketType;
import server.crypto.Encryption;

public class MessageReader implements Runnable {
	private final SSLSocket socket;

	boolean running_status;

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
				System.out.println("Received packet from client");
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
			
			if(ServerFiles.backup(p.get_sender(), filename, contents)) {
				response = new Packet("server", "", PacketType.ACK);
			}
			
			break;
		case PacketType.AVAILABLE_FILES:
			System.out.println("Processing available files request");
			String user = p.get_sender();
			String fileNames = ServerFiles.getAvailableFiles(user);
			System.out.println("Available files: " + fileNames);
			
			
			response = new Packet("server", fileNames, PacketType.AVAILABLE_FILES);
			
			break;
		default:
			System.out.println("Could not process packet. Did not recognize type " + p.get_packet_type().toString());
			break;
		}

		return response;
	}
}
