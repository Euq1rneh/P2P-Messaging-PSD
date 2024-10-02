package distribution.server;

import java.net.Socket;

public class RequestManager extends Thread{
	
	private final Socket cliSocket;
	
	public RequestManager(Socket cliSocket) {
		this.cliSocket = cliSocket;
	}
	
	@Override
	public void run() {
		
	}
}
