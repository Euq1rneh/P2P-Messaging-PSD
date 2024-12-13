package server.threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

import javax.net.ssl.SSLSocket;

import server.messages.MessageReader;
import server.messages.ServerMaps;

public class ConnectionAcceptorThread extends Thread {

	private boolean running;
	private ServerSocket socket;
	private ServerMaps sm;
	
	public ConnectionAcceptorThread(ServerSocket socket, boolean running, ServerMaps sm) {
		this.socket = socket;
		this.running = running;
		this.sm = sm;
	}

	@Override
	public void run() {

		while (!socket.isClosed()) {
			try {
				SSLSocket clientSocket = (SSLSocket) socket.accept();

				new Thread(new MessageReader(clientSocket, running, sm)).start();
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
