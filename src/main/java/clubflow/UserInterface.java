package clubflow;

import java.util.Scanner;

public class UserInterface {

    Scanner scanner;
    private String printPrefix;
    private String promptPrefix;

    public UserInterface(String printPrefix, String promptPrefix){
        scanner = new Scanner(System.in);
        this.printPrefix = printPrefix;
        this.promptPrefix = promptPrefix;
    }

    public void welcome(){
        //TODO Someone artistic please make a pretty welcome message
        System.out.println("Welcome to ClubFlow!");
    }

    public void print(String message){
        System.out.println(printPrefix + message);
    }

    public String prompt(){
        System.out.print(promptPrefix);
        return scanner.nextLine();
    }
}
