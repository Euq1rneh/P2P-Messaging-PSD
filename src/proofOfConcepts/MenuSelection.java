package proofOfConcepts;

import java.io.IOException;

import java.io.IOException;

public class MenuSelection {

    // Menu options
    static String[] options = { "Option 1", "Option 2", "Option 3", "Exit" };
    // Current selected index
    static int selectedIndex = 0;

    public static void main(String[] args) throws IOException {
        // Set terminal in raw mode (optional for Unix-like systems)
        // Run the menu loop
        while (true) {
            // Clear console and print the menu
            printMenu();

            // Check if there's input available without blocking
            if (System.in.available() > 0) {
                // Read input from the user
                int key = System.in.read();

                switch (key) {
                    case '+': // Plus key moves selection down
                        if (selectedIndex < options.length - 1) {
                            selectedIndex++;
                            System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
                        }
                        break;
                    case '-': // Minus key moves selection up
                        if (selectedIndex > 0) {
                            selectedIndex--;
                        }
                        break;
                    case '\n': // Enter key selects the current option
                    case '\r': // Enter key for Windows (sometimes '\r' is used for Enter)
                        System.out.println("You selected: " + options[selectedIndex]);
                        if (options[selectedIndex].equals("Exit")) {
                            System.out.println("Exiting...");
                            return;  // Exit the program
                        }
                        break;
                }
            }
        }
    }

    // Method to print the menu with the current selection highlighted
    private static void printMenu() {
        System.out.print("\033[H\033[2J");  // ANSI escape code to clear screen
        System.out.flush();
        for (int i = 0; i < options.length; i++) {
            if (i == selectedIndex) {
                System.out.println("> " + options[i]);
            } else {
                System.out.println("  " + options[i]);
            }
        }
    }
}
