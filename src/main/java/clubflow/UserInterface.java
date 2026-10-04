package clubflow;

import java.util.Scanner;

/**
 * Represents the User Interface of the app.
 * Manages the aesthetic and function of the printing and scanning of messages on the terminal screen.
 */
public class UserInterface {

    private Scanner scanner;
    private String printPrefix;
    private String promptPrefix;

    /**
     * Creates an instance of User Interface.
     * @param printPrefix Prefix to be added before printing a message on screen.
     *                    If printPrefix value is "A" and msg is "ABCDE", "AABCDE" would be printed.
     * @param promptPrefix String to be printed before reading user's input.
     */
    public UserInterface(String printPrefix, String promptPrefix){
        scanner = new Scanner(System.in);
        this.printPrefix = printPrefix;
        this.promptPrefix = promptPrefix;
    }

    /**
     * Prints welcome message on screen.
     */
    public void welcome(){
        //TODO Someone artistic please make a pretty welcome message
        System.out.println("Welcome to ClubFlow!");
    }

    /**
     * Prints a message on screen.
     * @param message Message to be printed.
     */
    public void print(String message){
        System.out.println(printPrefix + message);
    }

    /**
     * Reads user's input on screen.
     * @return User's line input.
     */
    public String prompt(){
        System.out.print(promptPrefix);
        return scanner.nextLine();
    }
}
