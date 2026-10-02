package clubflow;

import java.util.ArrayList;

public class CommandParser {

    private static final char QUOTE_CHAR = '\"';
    private static final char SPACE_CHAR = ' ';

    public CommandParser(UserInterface ui){
        //System.out.println(separate("This is       for testing seperate this \"but not this\" lol l a     "));
    }

    private ArrayList<String> separate(String s){
        return separate(null, s);
    }

    private ArrayList<String> separate(ArrayList<String> sepList, String s){
        ArrayList<String> resultSepList = (sepList == null? new ArrayList<String>() : sepList);
        String sTrim = s.trim();

        int quoteIndex = sTrim.indexOf(QUOTE_CHAR);
        int spaceIndex = sTrim.indexOf(SPACE_CHAR);
        if (quoteIndex != -1 && quoteIndex < spaceIndex){
            int nextQuoteIndex = sTrim.indexOf(QUOTE_CHAR, spaceIndex);
            if (nextQuoteIndex != -1) {
                spaceIndex = sTrim.indexOf(SPACE_CHAR, nextQuoteIndex);
            }
        }

        if (spaceIndex == -1){
            resultSepList.add(sTrim);
            return sepList;
        }

        resultSepList.add(sTrim.substring(0, spaceIndex));
        return separate(resultSepList, sTrim.substring(spaceIndex + 1));
    }

}

