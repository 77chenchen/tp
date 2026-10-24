//@@author czfray
package clubflow;

import java.util.Objects;
import java.util.Scanner;

/**
 * Represents the User Interface of the app.
 * Manages the aesthetic and function of the printing and scanning of messages on the terminal screen.
 */
public class UserInterface {
    /** Spaces added for each indentation level after the print prefix. */
    private static final String INDENT = " ".repeat(2);

    //@@author camimaigrot
    private static final String WELCOME_BANNER =
            "\u001B[38;2;197;197;197m                           ...........................\u001B[0m\n"
            +
            "\u001B[38;2;197;197;197m                    ......        ........................\u001B[0m\n"
            +
            "\u001B[38;2;197;197;197m               .....       ......                        ....::...\u001B[0m\n"
            +
            "\u001B[38;2;197;197;197m             ...       .:..             ............            ..::..\u001B" +
            "[0m\n" +
            "\u001B[38;2;197;197;197m      \u001B[38;2;24;0;170m#@@####\u001B[38;2;197;197;197m   \u001B[38;2;24" +
            ";0;170m@@@\u001B[38;2;197;197;197m   .:.      \u001B[38;2;24;0;170m@@@\u001B[38;2;197;197;197m  .::" +
            "  \u001B[38;2;24;0;170m##########\u001B[38;2;197;197;197m \u001B[38;2;24;0;170m#@@#\u001B[38;2;197;" +
            "197;197m           .:::\u001B[0m\n" +
            "\u001B[38;2;197;197;197m    \u001B[38;2;24;0;170m#@@#\u001B[38;2;197;197;197m    \u001B[38;2;24;0;1" +
            "70m#@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m   ::       \u001B[3" +
            "8;2;24;0;170m#@@\u001B[38;2;197;197;197m .:.    \u001B[38;2;24;0;170m#@@#\u001B[38;2;197;197;197m  " +
            " \u001B[38;2;24;0;170m#@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m@@#\u001B[38;2;197;197;197m:" +
            "           .::.\u001B[0m\n" +
            "\u001B[38;2;197;197;197m   \u001B[38;2;24;0;170m#@@#\u001B[38;2;197;197;197m    :.\u001B[38;2;24;0;" +
            "170m#\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;1" +
            "70m###\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m###\u001B[38;2;197;197;197m  \u001B[38;2;24;0;" +
            "170m#@@####\u001B[38;2;197;197;197m    \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m     \u001B[" +
            "38;2;24;0;170m#\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m@@#\u001B[38;2;197;197;197m::  \u001B" +
            "[38;2;24;0;170m###\u001B[38;2;197;197;197m   \u001B[38;2;24;0;170m####\u001B[38;2;197;197;197m:.   " +
            "  \u001B[38;2;24;0;170m##\u001B[0m\n" +
            "\u001B[38;2;197;197;197m   \u001B[38;2;24;0;170m@@@\u001B[38;2;197;197;197m     :.   \u001B[38;2;24" +
            ";0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m@@@\u001B[38;2;197;197;197m  \u001B[38;2;2" +
            "4;0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@#\u001B[38;2;197;197;197m:\u001B[38;2;" +
            "24;0;170m#@@#\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@#\u001B[38;2;197;197;197m  \u001B[38" +
            ";2;24;0;170m#\u001B[38;2;197;197;197m    \u001B[38;2;24;0;170m@@#\u001B[38;2;197;197;197m :\u001B[3" +
            "8;2;24;0;170m@@#\u001B[38;2;197;197;197m \u001B[38;2;24;0;170m@@#\u001B[38;2;197;197;197m \u001B[38" +
            ";2;24;0;170m#@@#\u001B[38;2;197;197;197m::\u001B[38;2;24;0;170m#@\u001B[38;2;197;197;197m   \u001B[" +
            "38;2;24;0;170m@\u001B[0m\n" +
            "\u001B[38;2;197;197;197m   \u001B[38;2;24;0;170m@@@\u001B[38;2;197;197;197m     :.   \u001B[38;2;24" +
            ";0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;2" +
            "4;0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m:: \u001B[38;2" +
            ";24;0;170m@@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@@##@#\u001B[38;2;197;197;197m   " +
            "\u001B[38;2;24;0;170m@@#\u001B[38;2;197;197;197m \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m  " +
            "\u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m:" +
            ":\u001B[38;2;24;0;170m@@#\u001B[38;2;197;197;197m \u001B[38;2;24;0;170m@#\u001B[0m\n" +
            "\u001B[38;2;197;197;197m   \u001B[38;2;24;0;170m@@@\u001B[38;2;197;197;197m     :.\u001B[38;2;24;0;" +
            "170m#\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;1" +
            "70m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;" +
            "170m#@@\u001B[38;2;197;197;197m.: \u001B[38;2;24;0;170m@@@\u001B[38;2;197;197;197m  \u001B[38;2;24;" +
            "0;170m#@@\u001B[38;2;197;197;197m   \u001B[38;2;24;0;170m#\u001B[38;2;197;197;197m    \u001B[38;2;2" +
            "4;0;170m@@#\u001B[38;2;197;197;197m \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;2" +
            "4;0;170m#@@\u001B[38;2;197;197;197m   \u001B[38;2;24;0;170m@@#@#@@#@\u001B[0m\n" +
            "\u001B[38;2;197;197;197m    \u001B[38;2;24;0;170m@@@\u001B[38;2;197;197;197m    .\u001B[38;2;24;0;1" +
            "70m#@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;1" +
            "70m#@@\u001B[38;2;197;197;197m. \u001B[38;2;24;0;170m#@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;" +
            "170m#@@\u001B[38;2;197;197;197m . \u001B[38;2;24;0;170m@@#\u001B[38;2;197;197;197m  \u001B[38;2;24;" +
            "0;170m#@@\u001B[38;2;197;197;197m..      \u001B[38;2;24;0;170m@@#\u001B[38;2;197;197;197m  \u001B[3" +
            "8;2;24;0;170m@@\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m@@@\u001B[38;2;197;197;197m   \u001B[" +
            "38;2;24;0;170m#@@#\u001B[38;2;197;197;197m.\u001B[38;2;24;0;170m#@@\u001B[0m\n" +
            "\u001B[38;2;197;197;197m     \u001B[38;2;24;0;170m#@@@###@#\u001B[38;2;197;197;197m  \u001B[38;2;24" +
            ";0;170m@@@#\u001B[38;2;197;197;197m  \u001B[38;2;24;0;170m@@@##@@#\u001B[38;2;197;197;197m  \u001B[" +
            "38;2;24;0;170m@@##@@#\u001B[38;2;197;197;197m. \u001B[38;2;24;0;170m#@@@@#\u001B[38;2;197;197;197m " +
            "    \u001B[38;2;24;0;170m#@@@\u001B[38;2;197;197;197m .\u001B[38;2;24;0;170m#@@#@#\u001B[38;2;197;1" +
            "97;197m     \u001B[38;2;24;0;170m@@\u001B[38;2;197;197;197m:  \u001B[38;2;24;0;170m@@\u001B[0m\n" +
            "\u001B[38;2;197;197;197m            .:.        .:::.           .......         .::.         .::." +
            "\u001B[0m\n" +
            "\u001B[38;2;197;197;197m              ....         ..::....               ......         .:::." +
            "\u001B[0m\n" +
            "\u001B[38;2;197;197;197m                  .....           ................          ..:::..\u001B[0" +
            "m\n" +
            "\u001B[38;2;197;197;197m                       ............              .....::::::..\u001B[0m\n";
    //@@author czfray
    private final Scanner scanner;
    private final String printPrefix;
    private final String promptPrefix;

    /**
     * Creates an instance of User Interface.
     * @param printPrefix Prefix to be added before printing a message on screen.
     *                    If printPrefix value is "A" and msg is "ABCDE", "AABCDE" would be printed.
     * @param promptPrefix String to be printed before reading user's input.
     */
    public UserInterface(String printPrefix, String promptPrefix) {
        scanner = new Scanner(System.in);
        this.printPrefix = Objects.requireNonNull(printPrefix, "Print prefix must not be null");
        this.promptPrefix = Objects.requireNonNull(promptPrefix, "Prompt prefix must not be null");
    }

    /**
     * Prints welcome message on screen.
     */
    public void welcome() {
        System.out.println(WELCOME_BANNER);
        System.out.println("Welcome to ClubFlow!");
        System.out.println("To get started, type \"help\" to explore the available commands.");
    }

    /**
     * Prints a message on screen.
     * @param message Message to be printed.
     */
    public void print(String message) {
        print(message, 0);
    }

    /**
     * Prints a message with two spaces per indentation level after the print prefix.
     *
     * @param message message to be printed
     * @param indentLevel non-negative indentation level; zero adds no indentation
     * @throws IllegalArgumentException if the indentation level is negative
     */
    public void print(String message, int indentLevel) {
        Objects.requireNonNull(message, "Printed message must not be null");
        if (indentLevel < 0) {
            throw new IllegalArgumentException("Indentation level must not be negative");
        }
        System.out.println(printPrefix + INDENT.repeat(indentLevel) + message);
    }

    /**
     * Reads user's input on screen.
     * @return User's line input.
     */
    public String prompt() {
        System.out.print(promptPrefix);
        return scanner.nextLine();
    }
}
