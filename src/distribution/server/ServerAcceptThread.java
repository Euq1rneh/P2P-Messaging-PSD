package distribution.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import peer.messages.MessageReader;

public class ServerAcceptThread extends Thread{

	private final ServerSocket s;
	private boolean running;
	
	public ServerAcceptThread(ServerSocket s, boolean running) {
		this.s = s;
		this.running = running;
	}
	
	@Override
	public void run() {
		while (!s.isClosed()) {
            try {
                Socket clientSocket = s.accept();
                System.out.println("Accepted connection from peer" + clientSocket.getInetAddress().getHostAddress());

                
                
                
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
