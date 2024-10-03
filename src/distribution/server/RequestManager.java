package distribution.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import peer.network.Packet;
import dataTypes.PacketType;

public class RequestManager extends Thread{
	
	private final Socket cliSocket;
	private boolean running;
	
	
	private ObjectOutputStream out;
	private ObjectInputStream in;
	
	public RequestManager(Socket cliSocket, boolean running) {
		this.cliSocket = cliSocket;
		
		// create the streams for communication
		System.out.println("--------> CREATING COMM STREAMS");
		try {
				out = new ObjectOutputStream(cliSocket.getOutputStream());
				in = new ObjectInputStream(cliSocket.getInputStream());
		} catch (IOException e) {
			System.out.println("Error creating communication streams");
			e.printStackTrace();
		}
		System.out.println("<-------- COMM STREAMS CREATED");
	}
	
	private Packet readPacket() {
		try {
			return (Packet) in.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	@Override
	public void run() {
		System.out.println("Client connection established");
		boolean closed = false;
		
		while(running && !closed) {
			Packet p = readPacket();
			
			if(p == null) {
				System.out.println("Error receiving packet");
				continue;
			}
			
			
			switch (p.get_packet_type()) {
			case PacketType.PEERS:
				System.out.println("Retrieving a list of connected peers");
				break;
			case PacketType.PK_RET:
				System.out.println("Retrieving pk from a peer");
				break;
			case PacketType.CLS:
				closed = true;
			default:
				break;
			}
		}
		
	}
}
