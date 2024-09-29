package peer;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

import peer.messages.MessageLogger;
import peer.network.ConnectionManager;

public class Main {

    static String RESET = "\u001B[0m";
    static String GREEN = "\u001B[32m";
    static String CYAN = "\u001B[46m";

    private static volatile boolean running = true;

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);  // Create a Scanner object
        System.out.print("Enter name: ");
        String userName = sc.nextLine();  // Read user input
        System.out.print("In Port: ");
        int in_port = sc.nextInt();
        System.out.print("Out Port: ");
        int out_port = sc.nextInt();
        

        Peer peer = new Peer(userName, in_port, out_port);

        peer.start(running);
        while(running) {
        	System.out.print("\033[H\033[2J");  // ANSI escape code to clear screen
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
    	  		peer.try_send_message();
        	}else {
        		System.out.printf("Command \"%s\" not recognized\n", command);
        	}
        	
        	
        }
        
//        while(true){
//            System.out.println(GREEN + "<----- Main thread start ----->" + RESET);
//            String command = sc.nextLine();
//            if(command.contains(":q")){
//                sc.close();
//                peer.close();
//                System.out.println("Closing app");
//                break;
//            } else if (command.contains(":c")) {
//                System.out.print(GREEN + "Peer address: " + RESET);
//                String peer_address = sc.nextLine();
//                System.out.print(GREEN + "Peer port: " + RESET);
//                int peer_port = sc.nextInt();
//                System.out.println(GREEN + "Starting connection test..." + RESET);
//                Socket s = ConnectionManager.try_connect_to_peer(peer_address, peer_port);
//                if(s == null){
//                    System.out.println(GREEN + "Could not establish connection..." + RESET);
//                    System.out.println(GREEN + "<----- Main thread end ----->" + RESET);
//                    continue;
//                }
//
//                System.out.println(GREEN + "Connection successful!" + RESET);
//                System.out.println(GREEN + "Closing connection" + RESET);
//
//                try {
//                    s.close();
//                }catch (IOException e) {
//                    System.out.println(GREEN + "Connection test finished with errors..." + RESET);
//                    System.exit(-1);
//                }
//
//                System.out.println(GREEN + "Connection test finished..." + RESET);
//            }else if (command.contains(":m")){
//                String[] command_args = command.split(" ");
//                if(command_args.length != 3){
//                    System.out.println(GREEN + "Incorrect number of arguments" + RESET);
//                    System.out.println(GREEN + "<----- Main thread end ----->" + RESET);
//                    continue;
//                }
//
//                //this block should disappear
//                //when opening a conversation the ip and port should be automatically retrieved
//                String peer_address = command_args[1];
//                int peer_port = Integer.parseInt(command_args[2]);
//
//                System.out.print(GREEN + "Message: " + RESET);
//                String message = sc.nextLine();
//
//                if(peer.send_message(message, peer_address, peer_port) == -1){
//                    System.out.println(GREEN + "Error sending message to peer" + RESET);
//                }
//            } else if (command.contains(":l")) {
//                peer.list_conversations();
//            }else if(command.contains(":o")) {
//            	String[] command_args = command.split(" ");
//            	if(command_args.length != 2) {
//            		System.out.println(GREEN + "Incorrect number of arguments" + RESET);
//                    System.out.println(GREEN + "<----- Main thread end ----->" + RESET);
//                    continue;
//            	}
//            	
//            	int conversation_id = Integer.parseInt(command_args[1]);
//            	peer.open_conversation(conversation_id);
//            	
//            }
//            sc.reset();
//            System.out.println(GREEN + "<----- Main thread end ----->" + RESET);
//        }
//        System.out.println("Exited loop");
    }
}
