package client.peer.threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import javax.net.ssl.SSLSocket;

import client.peer.Peer;
import client.peer.messages.MessageReader;

public class ConnectionAcceptorThread extends Thread {

	private final Peer peer;
	private boolean running;

	public ConnectionAcceptorThread(Peer peer, boolean running) {
		this.peer = peer;
		this.running = running;
	}

	@Override
	public void run() {

		ServerSocket server_socket = peer.getInputSocket();
		while (!server_socket.isClosed()) {
			try {
				SSLSocket clientSocket = (SSLSocket) server_socket.accept();
				//System.out.println("Accepted connection from peer" + clientSocket.getInetAddress().getHostAddress());

				new Thread(new MessageReader(peer, clientSocket, running)).start();
			} catch (SocketException e) {
				// this exception hopefully will only be thrown when quiting the program
				// so there is no need to handle the error
			} catch (IOException e) {
				System.out.println("Error accepting connection");
				e.printStackTrace();
			}
		}
	}

}
