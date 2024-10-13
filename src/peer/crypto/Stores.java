package peer.crypto;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Scanner;

public class Stores {

	private static int buildKeystore(String[] args) throws IOException, InterruptedException {
		// Command to generate the keystore
		String[] command = { "keytool", "-genkeypair", "-alias", args[8], "-keyalg", "RSA", "-keysize", "2048",
				"-keystore", args[9] + ".jceks", "-storetype", "JCEKS","-validity", "365" };

		// Start the process
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.redirectErrorStream(true); // Redirect error stream to output stream
		Process process = processBuilder.start();

		// Capture the output
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		OutputStream outputStream = process.getOutputStream();

		// Write inputs to the process
		outputStream.write((args[0] + "\n").getBytes()); // Keystore password
		outputStream.write((args[1] + "\n").getBytes()); // Re-enter keystore password
		outputStream.write((args[2] + "\n").getBytes()); // CN
		outputStream.write((args[3] + "\n").getBytes()); // OU
		outputStream.write((args[4] + "\n").getBytes()); // O
		outputStream.write((args[5] + "\n").getBytes()); // L
		outputStream.write((args[6] + "\n").getBytes()); // ST
		outputStream.write((args[7] + "\n").getBytes()); // C
		outputStream.write("yes\n".getBytes()); // Confirmation

		// Close output stream
		outputStream.flush();
		outputStream.close();

		// Read the output
		String line;
		while ((line = reader.readLine()) != null) {
			System.out.println(line);
		}

		// Wait for the process to complete
		int exitCode = process.waitFor();
		if (exitCode == 0) {
			System.out.println("Keystore generated successfully!");
			return 0;
		} else {
			System.out.println("Error generating keystore. Exit code: " + exitCode);
			return -1;
		}
	}

	public static KeyStore generateKeystore() {
		Scanner scanner = new Scanner(System.in);
		KeyStore k;
		
		
		String keystorePassword;
		String keystorePasswordConfirm;
		boolean match, metRequirements = false;

		do {
			System.out.print("Enter keystore password: ");
			keystorePassword = scanner.nextLine();

			System.out.print("Re-enter keystore password: ");
			keystorePasswordConfirm = scanner.nextLine();

			match = keystorePassword.equals(keystorePasswordConfirm);

			if (!match) {
				System.out.println("Passwords do not match.");
			}

			metRequirements = keystorePassword.length() >= 6;

			if (!metRequirements) {
				System.out.println("Keystore password is too short - must be at least 6 characters");
			}
			System.out.print("\033[H\033[2J"); // might not work (ANSI code for clearing the screen)
		} while (!match || !metRequirements);

		System.out.print("Enter your first and last name (CN): ");
		String commonName = scanner.nextLine();

		System.out.print("Enter your organizational unit (OU): ");
		String organizationalUnit = scanner.nextLine();

		System.out.print("Enter your organization (O): ");
		String organization = scanner.nextLine();

		System.out.print("Enter your city (L): ");
		String city = scanner.nextLine();

		System.out.print("Enter your state (ST): ");
		String state = scanner.nextLine();

		String countryCode;
		do {
			System.out.print("Enter your two-letter country code (C): ");
			countryCode = scanner.nextLine();

			metRequirements = countryCode.length() == 2;

			if (!metRequirements) {
				System.out.println("Country code must have two letters!");
			}
		} while (!metRequirements);

		System.out.print("Alias: ");
		String alias = scanner.nextLine();

		System.out.print("Filename (without file extension): ");
		String filename = scanner.nextLine();

		try {

			String[] args = new String[] { keystorePassword, keystorePasswordConfirm, commonName, organizationalUnit,
					organization, city, state, countryCode, alias, filename };

			if(buildKeystore(args) == -1) {
				System.out.println("Error generating keystore exiting program");
				System.exit(-1);
			}
			
			k = tryLoadKeystore(filename+".jceks", keystorePassword);
			if(k == null) {
				return null;
			}

		} catch (IOException | InterruptedException e) {
			return null;
		}
		
		return k;
	}

	public static KeyStore tryLoadKeystore(String path, String keystorePassword) {
				
		try (FileInputStream fis = new FileInputStream(path)) {
			KeyStore keystore = KeyStore.getInstance("JCEKS");
			keystore.load(fis, keystorePassword.toCharArray());
			return keystore;
		} catch (FileNotFoundException e) {
			System.out.println("Could not find the keystore at the specified path");
			return null;
		} catch (IOException e) {
			System.out.println("Error while trying to read keystore file");
			e.printStackTrace();
			return null;
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Could not find the specified algorithm");
			return null;
		} catch (CertificateException e) {
			System.out.println("Could not load a certificate from the keystore");
			return null;
		} catch (KeyStoreException e) {
			System.out.println("No provider supports a KeyStoreSpi implementation for JCEKS");
			return null;
		}
	}
}
