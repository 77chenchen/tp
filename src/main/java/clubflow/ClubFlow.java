package clubflow;

public class ClubFlow {

    UserInterface ui;
    CommandParser parser;

    public ClubFlow(){
        ui = new UserInterface("[CF] ", "> ");
        parser = new CommandParser(ui);
    }

    public void run(){
        ui.welcome();
        while (true){
            ui.print(ui.prompt());
        }
    }

    public static void main(String[] args){
        new ClubFlow().run();
    }
}
