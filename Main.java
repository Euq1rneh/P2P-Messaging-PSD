import Peer.Network.ConnectionManager;
import Peer.Peer;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);  // Create a Scanner object
        System.out.print("Enter name: ");
        String userName = sc.nextLine();  // Read user input
        System.out.print("In Port: ");
        int in_port = sc.nextInt();
//        System.out.print("Out Port: ");
//        int out_port = sc.nextInt();

        Peer peer = new Peer(userName, in_port);

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
                int peer_port = sc.nextInt();
                ConnectionManager.try_connect_to_peer(peer_address, peer_port);
            }
        }
    }
}
