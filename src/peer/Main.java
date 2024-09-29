package peer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

import peer.messages.MessageLogger;
import peer.network.ConnectionManager;

public class Main {

    static String RESET = "\u001B[0m";
    static String GREEN = "\u001B[32m";
    static String CYAN = "\u001B[46m";

    private static volatile boolean running = true;

    public static void main(String[] args) throws IOException {

        Scanner sc = new Scanner(System.in);  // Create a Scanner object
        System.out.print("Enter name: ");
        String userName = sc.nextLine();  // Read user input
        System.out.print("In Port: ");
        String in = sc.nextLine();
        int in_port = Integer.parseInt(in);
        System.out.print("Out Port: ");
        String out = sc.nextLine();
        int out_port = Integer.parseInt(out);
        
        Peer peer = new Peer(userName, in_port, out_port);

        peer.start(running);
        while(running) {
        	//System.out.print("\033[H\033[2J");  // ANSI escape code to clear screen
        	peer.list_conversations();
        	
        	String command = sc.nextLine();  
        	String[] command_args = command.split(" ");
        	

        	if(command_args[0].equals(":q")) {
        		sc.close();
    	  		peer.close();
    	  		break;
        	}else if(command_args[0].equals(":t")) {
        		String address = command_args[1];
    	  		int port = Integer.parseInt(command_args[2]);
    	  		peer.connect(address, port);
    	  		peer.try_send_message();
        	}else if(command_args[0].equals(":o")) {
        		int conversation_id = Integer.parseInt(command_args[1]);
    	  		peer.open_conversation(conversation_id);
    	  		// need to find a way to get the ip and port of a conversation with another peer
    	  		// MY IDEA: write the ip and port in the begining of the file since they'll most likely be encrypted
    	  		//peer.connect(address, port); 
    	  		//peer.try_send_message();
        	}else if(command.equals("")) {
        		//functions as an update to the terminal 
        	}
//        	else {
//        		System.out.printf("Command \"%s\" not recognized\n", command);
//        	}
        	try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
    }
}
