package server.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import javax.net.ssl.SSLSocket;

import common.EncryptedPacket;
import common.Packet;
import common.PacketType;
import server.cypto.Encryption;

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

				Packet response = processMessage(p);
				
				out.writeObject(Encryption.encryptPacket(p, p.get_sender()));
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

			break;
		case PacketType.BACKUP:
			response = new Packet("server", "", PacketType.ACK);
			break;
		default:
			System.out.println("Could not process packet. Did not recognize type " + p.get_packet_type().toString());
			break;
		}

		return response;
	}
}
