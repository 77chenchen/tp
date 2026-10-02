package clubflow;

public class CommandArg {
    private String id, value;

    public CommandArg(String id, String value){
        this.id = id;
        this.value = value;
    }

    public String getId(){
        return id;
    }

    public String getValue(){
        return value;
    }
}
