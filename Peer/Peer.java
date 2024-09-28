package Peer;

import Peer.Data.PacketType;
import Peer.Messages.MessageReader;
import Peer.Network.ConnectionManager;
import Peer.Network.Packet;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class Peer {
    private String name;
    private final int in_port;
    private final int out_port;
    private ServerSocket peer_in;
    private Socket peer_out;

    public Peer(String name, int in_port, int out_port){
        this.name = name;
        this.in_port = in_port;
        this.out_port = out_port;
    }

    /**
     * Sends the specified message to the specified peer (address + port)
     * @param msg the message to send
     * @param peer_address the ip address of the peer that should receive the message
     * @param peer_port the port of the peer that should receive the message
     * @return 0 if there was no error -1 otherwise
     */
    public int send_message(String msg, String peer_address, int peer_port){
        if(msg == null || msg.isEmpty() || msg.isBlank()){
            System.out.println("No message was provided");
            return -1;
        }

        //############### This logic should be in the open_conversation method #######################
        //should check if the ip and port match if so there is no need to disconect
        if(peer_out != null && peer_out.isConnected()){
            try {
                peer_out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        peer_out = ConnectionManager.try_connect_to_peer(peer_address, peer_port);

        if(peer_out == null){
            System.out.printf("Could not establish connection to peer (%s:%d)\n", peer_address, peer_port);
            return -1;
        }
        System.out.println("Connection successful");

        Packet p = new Packet(name, msg, PacketType.MSG);

        if(ConnectionManager.sendPacket(p, peer_out) == 0){
            //write message to file
            System.out.println("Writing message to file");
            //TODO write message to file
        }

        return 0;
    }

    /**
     * Open a conversation with a peer if a conversation does not exist it creates a new one.
     * This method is also responsible for connecting to the peer?????
     * @param conversation_id the id of the conversation to open
     * @return 0 if could open the conversation -1 in case of error
     */
    public int open_conversation(int conversation_id){
        //should connection only be established when sending a message ???????

        return -1;
    }

    public void list_conversations(){

    }

    /**
     * Starts the peer by opening a socket that is responsible for accepting connections and reading the incoming messages
     */
    public void start(boolean running){
        // Create a thread to handle accepting connections
        Thread connectionAcceptorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try (ServerSocket serverSocket = ConnectionManager.peer_server(in_port)) {
                    peer_in = serverSocket;
                    while (!serverSocket.isClosed()) {
                        try {
                            Socket clientSocket = serverSocket.accept();
                            System.out.println("Accepted connection from peer" + clientSocket.getInetAddress().getHostAddress());

                            new Thread(new MessageReader(clientSocket, running)).start();
                        } catch (SocketException e) {
                            // this exception hopefully will only be thrown when quiting the program
                            // so there is no need to handle the error
                        }catch (IOException e) {
                            System.out.println("Error accepting connection");
                            e.printStackTrace();
                        }
                    }
                    System.out.println("Server socket closed");
                } catch (IOException e) {
                    System.out.println("There was an error while trying to start peer");
                    e.printStackTrace();
                }
            }
        });

        // Start the thread that accepts connections
        connectionAcceptorThread.start();
    }

    public void close(){
        ConnectionManager.close_socket(peer_out);
        ConnectionManager.close_socket(peer_in);
    }
}
