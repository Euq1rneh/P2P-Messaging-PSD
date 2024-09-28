package Peer.Messages;

import java.io.*;
import java.nio.file.FileSystems;

public class MessageLogger {

    private static final String CONVERSATION_DIR_PATH = "conversations" + FileSystems.getDefault().getSeparator();
    private static final String CONVERSATION_DIR_NAME = "conversations";

    public static void build_conversation_dir(){
        File dir = new File(CONVERSATION_DIR_NAME);

        if (!dir.exists()) {
            boolean created = dir.mkdir();
            if (!created) {
                System.out.println("Failed to create the directory.");
            }
        }
    }

    /**
     * Write a message to a message file associated with a peer conversation
     * @param message the message to write
     * @param filename the file where the message should be written
     */
    public static void write_message_log(String message, String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(CONVERSATION_DIR_PATH + filename, true))) {
            writer.write(message);
            writer.newLine();
        } catch (IOException e) {
            // Handle any IOException that occurs while writing to the file
            System.err.println("An error occurred while writing the message to the file: " + e.getMessage());
        }
    }

    /**
     * Reads the message file
     * @param filename the name of the file
     */
    public static String read_message_log(String filename) {
        StringBuilder messageLog = new StringBuilder();

        // Use try-with-resources to ensure the file is closed automatically
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                messageLog.append(line).append(System.lineSeparator()); // Preserve line breaks
            }
        } catch (IOException e) {
            // Handle any IOException that occurs while reading the file
            System.err.println("An error occurred while reading the message log: " + e.getMessage());
        }

        return messageLog.toString();
    }
}
