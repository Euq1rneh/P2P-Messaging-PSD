package peer.threads;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;


import peer.messages.MessageReader;

public class ConnectionAcceptorThread extends Thread{

	private final String name;
	private boolean running;
	private final ServerSocket server_socket;
	
	public ConnectionAcceptorThread(String name, boolean running, ServerSocket server_socket) {
		this.name = name;
		this.running = running;
		this.server_socket = server_socket;
	}
	
	@Override
    public void run() {
            while (!server_socket.isClosed()) {
                try {
                    Socket clientSocket = server_socket.accept();
                    System.out.println("Accepted connection from peer" + clientSocket.getInetAddress().getHostAddress());

                    new Thread(new MessageReader(name, clientSocket, running)).start();
                } catch (SocketException e) {
                    // this exception hopefully will only be thrown when quiting the program
                    // so there is no need to handle the error
                }catch (IOException e) {
                    System.out.println("Error accepting connection");
                    e.printStackTrace();
                }
            }
            System.out.println("Server socket closed");
    }

}
