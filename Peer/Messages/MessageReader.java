package Peer.Messages;
import Peer.Data.PacketType;
import Peer.Network.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MessageReader implements Runnable {
    private final Socket socket;

    String RESET = "\u001B[0m";
    String YELLOW = "\u001B[33m";

    boolean running_status;

    public MessageReader(Socket socket, boolean running_status) {
        this.socket = socket;
        this.running_status = running_status;
    }

    @Override
    public void run() {
        try {
            boolean run = true;
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
            Packet packet;

            while (running_status) {
                System.out.println(YELLOW + "<------ Reading thread ------>"+ RESET);

                packet = (Packet) in.readObject();
                System.out.println(YELLOW + packet.get_sender() + ": " + packet.get_data() + RESET);
                out.writeObject(new Packet(null, null, PacketType.ACK));
                System.out.println(YELLOW + "<------ End of reading thread ------>" + RESET);
                System.out.println("Writing message to file");
                //TODO change file name
                MessageLogger.write_message_log(packet.get_sender() + ": " + packet.get_data(), "teste");
            }
        } catch (IOException e) {
            System.out.println("Peer connection may have been closed unexpectedly");
        } catch (ClassNotFoundException e) {
            System.out.println("Error: Packet class not found - " + e.getMessage());
        }
    }
}
