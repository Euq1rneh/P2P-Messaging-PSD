package distribution.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Server {

	public static volatile boolean running = true;
	
    public static ServerSocket create_server_socket(int port){
        try {
           return new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("<-----Error creating peer input socket----->");
            return null;
        }
    }
	
    
    private static void acceptClients(ServerDataController sc, ServerSocket srvSocket) {
		//SSLSocket cliSocket;
		Socket cliSocket;

		while (true) {
			try {
				//cliSocket = (SSLSocket) srvSocket.accept();
				cliSocket = (Socket) srvSocket.accept();
				
				RequestManager newThread = new RequestManager(sc, cliSocket, running);
				newThread.start();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
    }
	
	public static void main(String[] args) {
		
		if(args.length != 1) {
			System.out.println("Incorrect number of arguments");
			return;
		}
		
		int port = Integer.parseInt(args[0]);
		
		ServerSocket s = create_server_socket(port);
		
		if(s == null) {
			System.out.println("Error creating server socket");
			return;
		}
		
		ServerDataController serverDataController = new ServerDataController();
		
		acceptClients(serverDataController, s);

	}

}
