package peer;

import java.io.Console;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Scanner;

import peer.crypto.Stores;

public class Main {

	static String RESET = "\u001B[0m";
	static String GREEN = "\u001B[32m";
	static String CYAN = "\u001B[46m";

	private static volatile boolean running = true;
	private static KeyStore keystore;
	private static KeyStore truststore;
	
	private static String keyStoreConfiguration(Scanner sc) {
		String response =null;
		String password = "";
		System.out.println("> Do you already have a keystore you would like to use?(y/N)");
    	response = sc.nextLine();
    	
    	switch (response) {
    	case "":
    	case "Y":
		case "y":
			System.out.println("Starting load process...");
			System.out.print("> Filepath:");
			String path = sc.nextLine();
			System.out.print("> Password:");
			password = sc.nextLine();
			System.out.println("> Trying to load keystore...");
			keystore = Stores.tryLoadKeystore(path, password);
			
			if(keystore == null) {
				System.out.println("> Exiting programm");
				System.exit(-1);
			}
			System.out.println("> Keystore loading process finished successfully\n> Finishing configuration...");
			break;
		case "N":
		case "n":
			System.out.println("> Starting keystore generation process...");
			keystore = Stores.generateKeystore();
			if(keystore == null) {
				System.out.println("> Error while trying to generate keystore\n> Exiting programm");
				System.exit(-1);
			}
			System.out.println("> Keystore generation process finished successfully\n> Finishing configuration...");
			break;
		}
    	return password;
	}
	
	private static void loadAditionalCerts(Scanner sc, String password) {
		String certDirPath = "";
		String response = "";
		
		System.out.println("> Do you have certificates you would like to load?(y/N)");
		response = sc.nextLine();
		
		switch (response) {
    	case "":
    	case "Y":
		case "y":
			System.out.println("> Directory Path:");
			certDirPath = sc.nextLine();
			
			Stores.addCertificates(truststore, certDirPath, password);
			break;
		case "N":
		case "n":
			
			break;
		}
		
	}
	
	private static String trustStoreConfiguration(Scanner sc) {
		String response ="";
		String password = "";
		System.out.println("> Do you already have a truststore you would like to use?(y/N)");
    	response = sc.nextLine();
    	
    	switch (response) {
    	case "":
    	case "Y":
		case "y":
			System.out.println("Starting load process...");
			System.out.print("> Filepath:");
			String path = sc.nextLine();
			System.out.print("> Password:");
			password = sc.nextLine();
			System.out.println("> Trying to load truststore...");
			truststore = Stores.tryLoadTrustStore(path, password);
			
			if(truststore == null) {
				System.out.println("> Exiting programm");
				System.exit(-1);
			}
			
			loadAditionalCerts(sc, password);
			Stores.saveTrustStore(truststore, path, password);
			System.out.println("> Truststore loading process finished successfully\n> Finishing configuration...");
			break;
		case "N":
		case "n":
			System.out.println("> Starting truststore generation process...");
			truststore = Stores.generateTrustStore();
			if(truststore == null) {
				System.out.println("> Error while trying to generate truststore\n> Exiting programm");
				System.exit(-1);
			}
			System.out.println("> Filepath:");
			response = sc.nextLine();
			System.out.println("> Password (can be empty, this is not recommended):");
			password = sc.nextLine();
			
			loadAditionalCerts(sc, password);
			
			Stores.saveTrustStore(truststore, response, password);
			System.out.println("> Truststore generation process finished successfully\n> Finishing configuration...");
			break;
		}
    	return password;
	}
	
	private static String[] configuration(Scanner sc) {
		
		String[] passwords = new String[2];
		
    	System.out.println("#################>Configuration<#################");
    	passwords[0] = keyStoreConfiguration(sc);
    	passwords[1] = trustStoreConfiguration(sc);
    	System.out.println("#################################################");
    	//System.out.print("\033[H\033[2J"); // works with git bash(ANSI code for clearing the screen)
    	return passwords;
    }

	public static void main(String[] args) throws IOException {
		Scanner sc = new Scanner(System.in); // Create a Scanner object

		String[] passwords = configuration(sc);
		
		System.out.print("Enter name: ");
		String userName = sc.nextLine(); // Read user input

		System.out.print("In Port: ");
		String in = sc.nextLine();
		int in_port = Integer.parseInt(in);
		System.out.print("Out Port: ");
		String out = sc.nextLine(); // it need to be parsed like a string to avoid printing the initial menu twice
		int out_port = Integer.parseInt(out);

		Peer peer = new Peer(userName, in_port, out_port, keystore, truststore, passwords[0]);

		peer.start(running, keystore, passwords[0]);
		
		while (running) {
			peer.list_conversations();

			String command = sc.nextLine();
			String[] command_args = command.split(" ");

			if (command_args[0].equals(":q")) {
				sc.close();
				peer.close();
				break;
			} else if (command_args[0].equals(":t")) {
				String address = command_args[1];
				int port = Integer.parseInt(command_args[2]);
				peer.connect(address, port);
				peer.try_send_message();
			} else if (command_args[0].equals(":o")) {
				int conversation_id = Integer.parseInt(command_args[1]);
				peer.open_conversation(conversation_id);
				// need to find a way to get the ip and port of a conversation with another peer
				// MY IDEA: write the ip and port in the begining of the file since they'll most
				// likely be encrypted
				// peer.connect(address, port);
				// peer.try_send_message();
			} else if (command.equals("")) {
				// functions as an update to the terminal
			}
//        	else {
//        		System.out.printf("Command \"%s\" not recognized\n", command);
//        	}
//        	try {
//				Thread.sleep(100);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
		}
	}
}
