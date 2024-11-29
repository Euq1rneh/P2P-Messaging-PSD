package server.messages;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ServerFiles {

	private static final String FILE_SEPARATOR = File.separator;
	private static final String ROOT_DIRECTORY = "."+FILE_SEPARATOR+"conversations";

	/**
	 * Creates a conversation directory to store client backup files Each client
	 * will have its own directory. This method only creates the root directory.
	 */
	public static void createDirs() {
		File rootDir = new File(ROOT_DIRECTORY);

		if (!rootDir.exists()) {
			boolean isCreated = rootDir.mkdirs();

			if (isCreated) {
				System.out.println("Root directory created successfully");
			} else {
				System.err.println("Failed to create root directory");
			}
		} else {
			System.out.println("Root directory already exists");
		}
	}

	/**
	 * Checks if a user has a directory in the server
	 * @param user the user's name
	 * @return true if the directory exists false otherwise
	 */
	private static boolean userDirExists(String user) {
		File userDir = new File(ROOT_DIRECTORY + FILE_SEPARATOR + user);

		if (!userDir.exists()) {
			System.out.println("User directory did not exist. Creating directory...");
			boolean isCreated = userDir.mkdirs(); // Create the root directory

			if (isCreated) {
				System.out.println("User directory created successfully");
			} else {
				System.err.println("Failed to create user directory");
			}
			return false;
		}

		return true;
	}

	/**
	 * Returns the contents of the user requested file
	 * 
	 * @param user     the user's name
	 * @param filename the name of the requested file
	 * @return the contents of the file if they exist, null otherwise
	 */
	public static String retrieve(String user, String filename) {

		if (userDirExists(user)) {
			File requestedFile = new File(ROOT_DIRECTORY + FILE_SEPARATOR + user + FILE_SEPARATOR + filename);

			if (!requestedFile.exists()) {
				return null;
			}

			StringBuilder fileContents = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new FileReader(requestedFile))) {
				String line;
				while ((line = reader.readLine()) != null) {
					fileContents.append(line).append(System.lineSeparator());
				}
			} catch (IOException e) {
				System.err.println("Error reading the file: " + e.getMessage());
				return null;
			}

			return fileContents.toString().trim(); // Remove trailing newline
		}

		return null;
	}
	
	/**
	 * Creates a backup of the file. If it already exists replaces it with a new copy
	 * @param user the name of the user that is saving the file (used for searching for their directory)
	 * @param filename the name of the file
	 * @param content the encrypted contents of the file
	 * @return true if succeeded false otherwise
	 */
	public static boolean backup(String user, String filename, String content) {
	    try {
	        if (userDirExists(user)) {
	            File backupFile = new File(ROOT_DIRECTORY + FILE_SEPARATOR + user + FILE_SEPARATOR + filename);
	            
	            try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))) {
	                writer.write(content);
	            }
	            
	            return true;
	        } else {
	            File userDir = new File(ROOT_DIRECTORY + FILE_SEPARATOR + user);
	            if (userDir.mkdirs()) {
	                File backupFile = new File(userDir, filename);
	                try (BufferedWriter writer = new BufferedWriter(new FileWriter(backupFile))) {
	                    writer.write(content);
	                }
	                
	                return true;
	            }
	        }
	    } catch (IOException e) {
	        System.err.println("Error creating backup: " + e.getMessage());
	    }
	    
	    return false;
	}

	/**
	 * retrieves the names of any available files per user
	 * @param user the name of the users that is requesting his files
	 * @return the names of the files separated by spaces
	 */
	public static String getAvailableFiles(String user) {
	    if (!userDirExists(user)) {
	        return "";
	    }
	    
	    File userDir = new File(ROOT_DIRECTORY + FILE_SEPARATOR + user);
	    File[] files = userDir.listFiles();
	    
	    if (files == null || files.length == 0) {
	        return "";
	    }
	    
	    StringBuilder fileNames = new StringBuilder();
	    for (File file : files) {
	        if (file.isFile()) {
	            fileNames.append(file.getName()).append(" ");
	        }
	    }
	    
	    return fileNames.toString().trim();
	}



}
