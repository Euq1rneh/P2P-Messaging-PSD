package server.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import client.dataTypes.PacketType;
import client.peer.Peer;
import client.peer.network.Packet;
import common.EncryptedPacket;

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
				System.out.println("Saving...");
				
			}
		} catch (IOException e) {
			System.out.println("Client connection closed");
		} catch (ClassNotFoundException e) {
			System.out.println("Error: Packet class not found - " + e.getMessage());
		}
	}
}
