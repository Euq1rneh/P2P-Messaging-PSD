package server.threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

import javax.net.ssl.SSLSocket;

import server.messages.MessageReader;

public class ConnectionAcceptorThread extends Thread {

	private boolean running;
	private ServerSocket socket;
	
	public ConnectionAcceptorThread(ServerSocket socket, boolean running) {
		this.socket = socket;
		this.running = running;
	}

	@Override
	public void run() {

		while (!socket.isClosed()) {
			try {
				SSLSocket clientSocket = (SSLSocket) socket.accept();

				new Thread(new MessageReader(clientSocket, running)).start();
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
