import Peer.Peer;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);  // Create a Scanner object
        System.out.print("Enter name: ");
        String userName = sc.nextLine();  // Read user input
        System.out.print("Port: ");
        int port = sc.nextInt();

        sc.close();

        Peer peer = new Peer(userName, port);

        peer.start();
    }
}
