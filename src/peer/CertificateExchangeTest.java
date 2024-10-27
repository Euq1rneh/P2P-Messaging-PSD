package peer;

import java.security.KeyStore;
import java.util.Scanner;

import peer.crypto.Stores;

public class CertificateExchangeTest {
	public static void main(String[] args) throws InterruptedException {
		Scanner sc = new Scanner(System.in);
		
		System.out.print("Keystore path:");
		String keyStorePath = sc.nextLine();
		System.out.print("Keystore password:");
		String password = sc.nextLine();
		
		String inPort = sc.nextLine();
		
		KeyStore Astore = Stores.tryLoadKeystore(keyStorePath, password);
		KeyStore Atrust = Stores.generateTrustStore();
		
		Peer p = new Peer("A", Integer.parseInt(inPort), 0, Astore, Atrust, password);
		
		boolean running = true;
		
		p.start(running, Astore, password);
		
		while(true) {
			System.out.print("Command:");
			String command = sc.nextLine();
			String[] arg = command.split(" ");
			
			if(arg[0].equals(":c")) {
				p.connect(arg[1], Integer.parseInt(arg[2]));
			}else if(arg[0].equals(":q")) {
				p.close();
				break;
			}
		}
		
		
	}
}
