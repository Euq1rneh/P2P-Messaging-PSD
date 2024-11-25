package client.peer.messages;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

import client.peer.Peer;
import common.EncryptedPacket;
import common.Packet;
import common.PacketType;

public class MessageReader implements Runnable {
	private final SSLSocket socket;
	private final Peer peer;
	String RESET = "\u001B[0m";
	String YELLOW = "\u001B[33m";

	boolean running_status;

	public MessageReader(Peer peer, SSLSocket socket, boolean running_status) {
		this.peer = peer;
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

				System.out.println("Receiving message");
				Packet msg = this.peer.tryReadMessage(packet);
				if (msg == null) {
					System.out.println("Error receiving message");
					continue;
				}

				System.out.println("Sending ACK");
//                System.out.println(YELLOW + msg.get_sender() + ": " + msg.get_data() + RESET);
                EncryptedPacket encAck = this.peer.encryptPacket(msg.get_sender(), "", PacketType.ACK);
                if(encAck == null) {
                	System.out.println("Error encrypting ACK packet");
                	System.exit(-1);
                }
                
                out.writeObject(encAck); // there is no need to encrypt this packet????

//				System.out.println(YELLOW + "<------ End of reading thread ------>" + RESET);
//				System.out.println("Writing message to file");
				// TODO change file name
                System.out.println("Logging message");
                MessageLogger.write_message_log(msg.get_sender() + ": " + msg.get_data(), msg.get_sender() + ".conversation");
			}
		} catch (IOException e) {
			System.out.println("Peer connection closed");
		} catch (ClassNotFoundException e) {
			System.out.println("Error: Packet class not found - " + e.getMessage());
		}
	}
}
