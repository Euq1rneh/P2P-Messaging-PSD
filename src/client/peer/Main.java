package client.peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Properties;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import common.Stores;

public class Main {

	private static volatile boolean running = true;
	private static KeyStore keystore;
	private static KeyStore truststore;

	private static boolean canClearTerminal = true;
	private static boolean hasINIFile = false;

	private static String username;
	private static String keystorePath;
	private static String password;
	private static String truststorePath;
	private static int port;
	private static String truststorePassword;

	private static void clearTerminal() {
		try {
			if (!canClearTerminal)
				return;

			new ProcessBuilder("clear").inheritIO().start().waitFor();
		} catch (Exception e) {
			System.out.println(
					"Could not clear terminal. Programm will not try to clear terminal again during this execution");
			canClearTerminal = false;
		}
		;
	}

	private static String keyStoreConfiguration(Scanner sc) {
		String response = null;
		String password = "";
		// System.out.println("> Do you already have a keystore you would like to
		// use?(y/N)");
//    	response = sc.nextLine();
		response = "y";

		switch (response) {
		case "":
		case "Y":
		case "y":
			System.out.println("Starting KeyStore load process...");
			System.out.print("> Filepath:");
			String path = sc.nextLine();
			System.out.print("> Password:");
			password = sc.nextLine();
			System.out.println("> Trying to load keystore...");
			keystore = Stores.tryLoadKeystore(path, password);

			if (keystore == null) {
				System.out.println("> Exiting programm");
				System.exit(-1);
			}
			System.out.println("> Keystore loading process finished successfully\n> Finishing configuration...");
			break;
		case "N":
		case "n":
			System.out.println("> Starting keystore generation process...");
			keystore = Stores.generateKeystore(sc);
			if (keystore == null) {
				System.out.println("> Error while trying to generate keystore\n> Exiting programm");
				System.exit(-1);
			}
			System.out.println(
					"> Keystore generation process finished successfully\n> Finishing KeyStore configuration...");
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
		String response = "";
		String password = "";
		// System.out.println("> Do you already have a truststore you would like to
		// use?(y/N)");
		// response = sc.nextLine();

		response = "y";

		switch (response) {
		case "":
		case "Y":
		case "y":
			System.out.println("Starting TrustStore load process...");
			System.out.print("> Filepath:");
			String path = sc.nextLine();
			System.out.print("> Password (can be empty):");
			password = sc.nextLine();
			System.out.println("> Trying to load truststore...");
			truststore = Stores.tryLoadTrustStore(path, password);

			if (truststore == null) {
				System.out.println("> Exiting programm");
				System.exit(-1);
			}

//			loadAditionalCerts(sc, password);
//			Stores.saveTrustStore(truststore, path, password);
			System.out.println("> Truststore loading process finished successfully\n> Finishing configuration...");
			break;
		case "N":
		case "n":
			System.out.println("> Starting truststore generation process...");
			truststore = Stores.generateTrustStore();
			if (truststore == null) {
				System.out.println("> Error while trying to generate truststore\n> Exiting programm");
				System.exit(-1);
			}
			System.out.println("> Filepath:");
			response = sc.nextLine();
			password = "";

			loadAditionalCerts(sc, password);

			Stores.saveTrustStore(truststore, response, password);
			System.out.println(
					"> Truststore generation process finished successfully\n> Finishing TrustStore configuration...");
			break;
		}
		return password;
	}

	/**
	 * Reads the INI file that contains the necessary values to start the program
	 * automatically
	 */
	private static boolean readConfigFromINI() {
		File iniFile = new File("config.ini");

		System.out.println("> Checking for INI file...");

		if (iniFile.exists()) {
			hasINIFile = true;
			System.out.println("> INI file found reading properties...");
			Properties properties = new Properties();
			try (FileInputStream fis = new FileInputStream(iniFile)) {
				properties.load(fis);

				// Read the values from the properties file
				keystorePath = properties.getProperty("keystorePath");
				password = properties.getProperty("keystorePassword");
				truststorePath = properties.getProperty("truststorePath");
				truststorePassword = properties.getProperty("truststorePassword");

				// this field is not mandatory
				if (truststorePassword == null) {
					truststorePassword = "";
				}

				username = properties.getProperty("username");
				String portString = properties.getProperty("port");

				// Convert port to an integer (handle potential exception)
				if (portString != null) {
					try {
						port = Integer.parseInt(portString);
					} catch (NumberFormatException e) {
						System.err.println("Invalid port number in config file.");
						port = -1; // Set port to an invalid value to indicate error
						return false;
					}
				}

				// If any of the required values are missing, print an error
				if (keystorePath == null || password == null || truststorePath == null || username == null
						|| port == -1) {
					System.err.println("Missing required configurations in the config file.");
					return false;
				}
			} catch (IOException e) {
				System.err.println("Error reading the configuration file: " + e.getMessage());
				return false;
			}

			return true;
		} else {
			System.out.println("> INI file not found");
			return false;
		}
	}

	private static String configuration(Scanner sc) {

		if (readConfigFromINI()) {
			System.out.println("> Trying to load keystore...");
			keystore = Stores.tryLoadKeystore(keystorePath, password);

			if (keystore == null) {
				System.out.println("> Exiting programm");
				System.exit(-1);
			}
			System.out.println("> Keystore loading process finished successfully\n> Finishing configuration...");

			System.out.println("> Trying to load truststore...");
			truststore = Stores.tryLoadTrustStore(truststorePath, truststorePassword);

			if (truststore == null) {
				System.out.println("> Exiting programm");
				System.exit(-1);
			}

			System.out.println("> Truststore loading process finished successfully\n> Finishing configuration...");

		} else {
			System.out.println("#################>Configuration<#################");
			password = keyStoreConfiguration(sc);
			trustStoreConfiguration(sc);
			System.out.println("#################################################");
			// System.out.print("\033[H\033[2J"); // works with git bash(ANSI code for
			// clearing the screen)
		}

		return password;
	}

	public static void main(String[] args) throws IOException {
		Scanner sc = new Scanner(System.in); // Create a Scanner object

		String password = configuration(sc);
		if (!hasINIFile) {

			System.out.print("Enter name: "); // should be derived from the key store name
			username = sc.nextLine(); // Read user input

			System.out.print("In Port: ");
			String in = sc.nextLine();
			port = Integer.parseInt(in);
		}

		Peer peer = new Peer(username, port, keystore, truststore, password);
		peer.createTrustManager(password, keystore, truststore);
		peer.start(running, keystore, password);

		password = "";

		clearTerminal();
		while (running) {
			peer.list_conversations();

			String command = sc.nextLine();
			String[] command_args = command.split(" ");

			if (command_args[0].equals(":q")) {
				sc.close();
				peer.close();
				break;
			} else if (command_args[0].equals(":t")) {
				
				if(command_args.length != 4) {
					System.out.println("Missing arguments for talk command: :t <ip> <port> <name>");
					continue;
				}
				
				String address = command_args[1];
				int port = 0;
				try {
					port = Integer.parseInt(command_args[2]);

				} catch (NumberFormatException e) {
					System.out
							.println("Second argument of the talk command should be the port\n:t <ip> <port> <alias>");
					continue;
				}
				String alias = command_args[3];

				clearTerminal();

				peer.connect(address, port);
				peer.try_send_message(sc, alias);
			} else if (command_args[0].equals(":o")) {
				
				if(command_args.length != 2) {
					System.out.println("Missing arguments for open command: :o <conversation ID>");
					continue;
				}
				
				int conversation_id = Integer.parseInt(command_args[1]);

				clearTerminal();
				peer.open_conversation(conversation_id);
				// need to find a way to get the ip and port of a conversation with another peer
				// MY IDEA: write the ip and port in the begining of the file since they'll most
				// likely be encrypted
				// peer.connect(address, port);
				// peer.try_send_message();
			} else if (command_args[0].equals(":s")) {
				if(command_args.length <= 1) {
					System.out.println("Missing arguments for search command: :s <keyword(s)>");
					continue;
				}
				try {
					peer.searchInConversations(command_args[1]);
				} catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
						| InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
					System.out.println("Error while trying to search for the term " + command_args[1]);
//					e.printStackTrace();
				}
			
			} else if (command.equals("")) {
				// functions as an update to the terminal
				clearTerminal();
			}
		}
	}
}
