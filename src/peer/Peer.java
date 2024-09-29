package peer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Scanner;

import peer.data.PacketType;
import peer.messages.MessageLogger;
import peer.messages.MessageReader;
import peer.network.ConnectionManager;
import peer.network.Packet;
import peer.threads.ConnectionAcceptorThread;

public class Peer {
    private String name;
    private final int in_port;
    private final int out_port;
    private ServerSocket peer_in;
    private Socket peer_out;
    //streams used for sending messages
    private ObjectOutputStream out; 
    private ObjectInputStream in;

    private String[] conversations;
    
    public Peer(String name, int in_port, int out_port){
        this.name = name;
        this.in_port = in_port;
        this.out_port = out_port;
    }

    /**
     * Connect to another peer using their Ip and port
     * @param address the ip address of the peer
     * @param port the port of the peer
     */
    public int connect(String address, int port) {
    	peer_out = ConnectionManager.try_connect_to_peer(address, port);
    	
    	if(peer_out == null) {
    		System.out.printf("Could not establish connection to peer (%s:%d)\n", address, port);
    		return -1;
    	}
    	
    	try {
			out = new ObjectOutputStream(peer_out.getOutputStream());
			in = new ObjectInputStream(peer_out.getInputStream());
		} catch (IOException e) {
			System.out.println("Could not create communication streams");
			return -1;
		}
    	
    	return 0;
    }
    
    public void try_send_message() {
    	boolean is_writing = true;
    	
    	
    	
    	try(BufferedReader br = new BufferedReader(new InputStreamReader(System.in))){
    		while(is_writing) {
        			System.out.print("Message > ");
    				String msg = br.readLine();
    				
    				switch (msg) {
    					case ":b":
    						is_writing = false;
    						break;
    				default:
    					// this could be changed later on if a server is used to store non delivered messages
    					if(send_message(msg, in, out) == -1) {
    						is_writing = false;
    						System.out.println("Error sending message or message was blank/empty");
    					}
    					break;
    				}
        	}	
    	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	
    	
    	
    }
    
    /**
     * Sends the specified message to the specified peer (address + port)
     * @param msg the message to send
     * @param peer_address the ip address of the peer that should receive the message
     * @param peer_port the port of the peer that should receive the message
     * @return 0 if there was no error -1 otherwise
     */
    private int send_message(String msg, ObjectInputStream in, ObjectOutputStream out){
        if(msg == null || msg.isEmpty() || msg.isBlank()){
            System.out.println("No message was provided");
            return -1;
        }

        if(peer_out == null){
            System.out.println("Could not send message beacause you're not connected to a peer");
            return -1;
        }

        Packet p = new Packet(name, msg, PacketType.MSG);        
        ConnectionManager.sendPacket(p, in, out);

        return 0;
    }

    /**
     * Open a conversation with a peer if a conversation does not exist it creates a new one.
     * This method is also responsible for connecting to the peer?????
     * @param conversation_id the id of the conversation to open
     * @return 0 if could open the conversation -1 in case of error
     */
    public int open_conversation(int conversation_id){
    	String conversation = MessageLogger.read_message_log(conversations[conversation_id] + ".conversation");
    	
    	if(conversation == null) {
    		return -1;
    	}
    	
    	System.out.println(conversation);
    	
        return 0;
    }

    public void list_conversations(){
        conversations = MessageLogger.get_conversations();
        System.out.println("----------- Conversations -----------");
        if(conversations == null){
            System.out.println("No conversations were found.");
            System.out.println("Try starting a conversation by connecting to a peer and sending a message.");
            System.out.println("------------------------------------");
            return;
        }

        for (int i = 0; i < conversations.length; i++) {
            System.out.printf("%d. %s\n", i, conversations[i]);
        }
        System.out.println("------------------------------------");
    }

    /**
     * Starts the peer by opening a socket that is responsible for accepting connections and reading the incoming messages
     */
    public void start(boolean running){
        //1. check for conversation dir
        //2. start thread for accepting connections

        MessageLogger.build_conversation_dir();
        peer_in = ConnectionManager.peer_server(in_port);
        
        if(peer_in == null) {
        	System.out.println("Error while trying to initialize peer");
        	System.exit(-1);
        }

        // Create a thread to handle accepting connections
        ConnectionAcceptorThread connectionAcceptorThread = new ConnectionAcceptorThread(name, running, peer_in);

        // Start the thread that accepts connections
        connectionAcceptorThread.start();
    }

    public void close(){
        ConnectionManager.close_socket(peer_out);
        ConnectionManager.close_socket(peer_in);
    }

}