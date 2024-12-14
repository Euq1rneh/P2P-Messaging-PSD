package server.messages;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import common.ByteArray;

public class ServerFiles {

	private static final String FILE_SEPARATOR = File.separator;
	private static final String ROOT_DIRECTORY = "."+FILE_SEPARATOR+"conversations";
	private static final String MAPS_DIRECTORY = "."+FILE_SEPARATOR+"searchMaps";
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
		
		File mapsDir = new File(MAPS_DIRECTORY);

		if (!mapsDir.exists()) {
			boolean isCreated = mapsDir.mkdirs();

			if (isCreated) {
				System.out.println("Maps directory created successfully");
			} else {
				System.err.println("Failed to create maps directory");
			}
		} else {
			System.out.println("Maps directory already exists");
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

	private static String readFile(File file) {
	    StringBuilder content = new StringBuilder();

	    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
	        String line;
	        while ((line = reader.readLine()) != null) {
	            content.append(line).append("\n");
	        }
	    } catch (IOException e) {
	        System.out.println("Error reading file: " + file.getName());
	    }

	    return content.toString();
	}

	private static HashMap<ByteArray, ByteArray> recreateMapFromString(String content) {
	    HashMap<ByteArray, ByteArray> map = new HashMap<>();

	    // Split the content by lines
	    String[] lines = content.split("\n");

	    for (String line : lines) {
	        // Skip empty lines
	        if (line.trim().isEmpty()) continue;

	        // Split by '@' to separate the key and value
	        String[] parts = line.split("@");
	        if (parts.length == 2) {
	            // Decode the Base64 strings back into ByteArrays
	            ByteArray key = new ByteArray(Base64.getDecoder().decode(parts[0]));
	            ByteArray value = new ByteArray(Base64.getDecoder().decode(parts[1]));

	            // Put them into the map
	            map.put(key, value);
	        }
	    }

	    return map;
	}

	public static HashMap<String, HashMap<ByteArray, ByteArray>> readFilesAndRecreateMaps() {
	    HashMap<String, HashMap<ByteArray, ByteArray>> recreatedMaps = new HashMap<>();

	    // The directory containing the searchmap files
	    File searchMapsDir = new File(MAPS_DIRECTORY);

	    // Check if the directory exists
	    if (!searchMapsDir.exists() || !searchMapsDir.isDirectory()) {
	        System.out.println("SearchMaps directory does not exist.");
	        return recreatedMaps;
	    }

	    System.out.println("Reading file in maps dir...");
	    // Iterate through all files in the "searchMaps" directory
	    for (File file : searchMapsDir.listFiles()) {
	        if (file.isFile() && file.getName().endsWith("-searchmap.dat")) {
	            // Extract the key from the filename (the part before "-searchmap.dat")
	            String key = file.getName().substring(0, file.getName().indexOf("-searchmap.dat"));
	            String content = readFile(file);
	            
	            System.out.println("New map for user " + key);
	            
	            HashMap<ByteArray, ByteArray> map = recreateMapFromString(content);
	            recreatedMaps.put(key, map);
	            
	            System.out.println("Number of entries for map is " + map.size());
	        }
	    }

	    return recreatedMaps;
	}
	
	private static void writeFile(String filename, String content) {
		// Check if the directory exists, if not, create it
		File file = new File(MAPS_DIRECTORY+FILE_SEPARATOR+filename);
		File mapsDir = new File(MAPS_DIRECTORY);

		if (mapsDir != null && !mapsDir.exists()) {
			boolean dirCreated = mapsDir.mkdirs(); // Create directories if they don't exist
			if (!dirCreated) {
				System.out.println("Failed to create directory: " + mapsDir.getAbsolutePath());
				return;
			}
		}

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("An error occurred while writing the message to the file: " + e.getMessage());
        }
	}

	private static String convertMapToSingleString(HashMap<ByteArray, ByteArray> map) {
		StringBuilder serializedMap = new StringBuilder();

		int size = map.size();
		int index = 0;

		for (Map.Entry<ByteArray, ByteArray> entry : map.entrySet()) {
			ByteArray key = entry.getKey();
			ByteArray val = entry.getValue();

			String serializedKey = Base64.getEncoder().encodeToString(key.getArr());
			String serializedValue = Base64.getEncoder().encodeToString(val.getArr());

			serializedMap.append(serializedKey + "@" + serializedValue);

			if (++index < size) {
				serializedMap.append("\n");
			}
		}

		return serializedMap.toString();
	}

	public static void convertSearchMapsToFile(HashMap<String, HashMap<ByteArray, ByteArray>> searchMaps) {
		for (Map.Entry<String, HashMap<ByteArray, ByteArray>> entry : searchMaps.entrySet()) {
			String key = entry.getKey();
			HashMap<ByteArray, ByteArray> val = entry.getValue();

			String content = convertMapToSingleString(val);
			writeFile(key+"-searchmap.dat", content);
		}
	}
}
