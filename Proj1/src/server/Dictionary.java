package server;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import utils.Actions;
import utils.Feedback;
import utils.FeedbackType;

import static java.lang.System.exit;

public class Dictionary {

    public static Dictionary dictionary;
    public String dict_path;
    private JSONObject my_dict;
    private HashMap<String, ArrayList<String>> symSpell = new HashMap<>();

    // if the path is not provided we can use the default one
    public Dictionary () {
        this.dict_path = "src/new_dictionary.json";
        loadFile();
    }

    public Dictionary (String dict_path) {
        this.dict_path = dict_path;
        loadFile();
    }

    public static Dictionary getDictionary() {
        if(dictionary == null) {
            dictionary = new Dictionary();
        }

        return dictionary;
    }

    public static Dictionary getDictionary(String dict_path) {
        if(dictionary == null) {
            dictionary = new Dictionary(dict_path);
        }
        return dictionary;
    }

    /**
     * write the edited dictionary back to the disk to preserve the changes
     * @return Feedback class contains whether it success or error sign and messages
     */
    public synchronized Feedback writeBack() {

        if (my_dict != null) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(dict_path))) {
                my_dict.write(writer);
                writer.write("\n");
                writer.flush();
                return new Feedback(FeedbackType.SUCCESS, "==== Write back successfully ====");
            } catch (IOException e) {
                return new Feedback(FeedbackType.ERROR, e.getMessage());
            }
        }

        return new Feedback(FeedbackType.SUCCESS, "==== dict is null, nothing to write ====");
    }

    private void loadFile() {
        try {
            FileReader file = new FileReader(this.dict_path);
            JSONTokener token = new JSONTokener(file);
            this.my_dict = new JSONObject(token);
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
            exit(0);
        } catch (JSONException e) {
            System.out.println("The dictionary is not in a standard json format: " + e.getMessage());
            exit(0);
        }

        // generate Symmetric Delete Spelling for quick word auto-correction
        for (Iterator<String> it = my_dict.keys(); it.hasNext(); ) {
            String word = it.next();
            HashSet<String> dSet;

            // only consider edit distance = 1 for now, can be extended if necessary
            dSet = generateDelete(word, 1);
            updateSysSpell(word, dSet);
        }
    }


    private static HashSet<String> generateDelete(String word, int d) {
        HashSet<String> res = new HashSet<>();

        if (d == 0 || (word.length() == 1)) {
            res.add(word);
            return res;
        }

        for (int i = 0; i < word.length(); i++) {
            String temp = word.substring(0,i) + word.substring(i+1);
            res.add(temp);
            res.addAll(generateDelete(temp, d-1));
        }

        return res;
    }

    private void updateSysSpell (String word, HashSet<String> dSet){
        for (String dWord: dSet) {
            if(symSpell.containsKey(dWord)) {
                symSpell.get(dWord).add(word);
            } else {
                ArrayList<String> temp = new ArrayList<>();
                temp.add(word);
                symSpell.put(dWord, temp);
            }
        }
    }


    /**
     * find a meaning of a word from dictionary
     * @param word the word you want to search
     * @return Feedback class contains either success with meaning or error with reason
     */
    public Feedback search(String word) {

        if (my_dict.has(word)){
            return new Feedback(FeedbackType.SUCCESS, my_dict.getString(word));
        } else {
            StringBuilder res = new StringBuilder();
            res.append("Word does not exist in the dictionary, " +
                    "please check the spell or add a meaning for the word!\n\n");
            res.append("------------- Suggestions -------------\n");

            res.append(fuzzySearch(word));

            return new Feedback(FeedbackType.ERROR, res.toString());
        }

    }

    private String fuzzySearch(String word) {
        StringBuilder res = new StringBuilder();

        if (symSpell.containsKey(word)) {
            ArrayList<String> fuzzyMatch = symSpell.get(word);

            for (String w:fuzzyMatch) {
                res.append(w).append(", ");
            }
        }

        HashSet<String> dSet;

        dSet = generateDelete(word, 1);

        buildFuzzy(dSet, res);

        if (res.length() != 0) {
            return res.toString();
        }

        return "No suggestion found!";
    }

    private void buildFuzzy(HashSet<String> dSet, StringBuilder res) {
        for (String dWord:dSet) {
            if (my_dict.has(dWord)) {
                res.append(dWord).append(", ");
            }

            if(symSpell.containsKey(dWord)) {
                for (String w: symSpell.get(dWord)) {
                    res.append(w).append(", ");
                }
            }
        }
    }

    /**
     * A universal method for processing any query that need to modify the dictionary in order to keep consistency
     * @param word the word of any supported query
     * @param info the info to add, which is dedicate for ADD query
     * @param mode query mode, can be found in utils.Actions
     * @return the feedback info contains the query success state and return message
     */
    public synchronized Feedback modify(String word, String info, Actions mode) {

        switch (mode) {
            case ADD:
                return add(word, info);
            case DEL:
                return delete(word);
            default:
                return new Feedback(FeedbackType.ERROR, "unknown mode");
        }
    }


    /**
     * add a word to dictionary with meaning and other extra information in a
     * json format string, note that the function is synchronized which only
     * on thread can add
     * @param word word you want to add to the dictionary
     * @param info meta-data about the word
     * @return  Feedback class contains either success or error with reason
     */
    private Feedback add(String word, String info){

        if (my_dict.has(word)) {
            return new Feedback(FeedbackType.ERROR, "Word already exist");
        }

        my_dict.put(word, info);
        return new Feedback(FeedbackType.SUCCESS, "Meaning added successfully");
    }

    // delete an word from the dictionary and remember to delete from disk
    // return either success or error message (word does not exist)
    private Feedback delete(String word) {

        if (my_dict.has(word)) {
            my_dict.remove(word);
            return new Feedback(FeedbackType.SUCCESS, "Delete successfully");
        }

        return new Feedback(FeedbackType.ERROR, "word does not exist");
    }

}
