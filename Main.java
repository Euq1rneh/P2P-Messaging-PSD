import Peer.Network.ConnectionManager;
import Peer.Peer;

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);  // Create a Scanner object
        System.out.print("Enter name: ");
        String userName = sc.nextLine();  // Read user input
        System.out.print("In Port: ");
        int in_port = sc.nextInt();
        System.out.print("Out Port: ");
        int out_port = sc.nextInt();

        Peer peer = new Peer(userName, in_port, out_port);

        peer.start();

        int close = 0;

        System.out.println("Entering command process loop");

        while(close == 0){
            System.out.println("Waiting for command");
            String command = sc.nextLine();
            if(command.contains(":q")){
                close = 1;
                sc.close();
            } else if (command.contains(":c")) {
                System.out.print("Peer address: ");
                String peer_address = sc.nextLine();
                System.out.print("Peer port: ");
                int peer_port = sc.nextInt();
                System.out.println("Starting connection test...");
                Socket s = ConnectionManager.try_connect_to_peer(peer_address, peer_port);
                if(s == null){
                    System.out.println("Could not establish connection...");
                    continue;
                }

                System.out.println("Connection successful!");
                System.out.println("Closing connection");

                try {
                    s.close();
                }catch (IOException e) {
                    System.out.println("Connection test finished with errors...");
                    System.exit(-1);
                }

                System.out.println("Connection test finished...");
            }else if (command.contains(":m")){
                String[] command_args = command.split(" ");
                if(command_args.length != 3){
                    System.out.println("Incorrect number of arguments");
                    continue;
                }

                //this block should disappear
                //when opening a conversation the ip and port should be automatically retrieved
                String peer_address = command_args[1];
                int peer_port = Integer.parseInt(command_args[2]);

                System.out.print("Message: ");
                String message = sc.nextLine();

                if(peer.send_message(message, peer_address, peer_port) == -1){
                    System.out.println("Error sending message to peer");
                }
            }
        }
    }
}
