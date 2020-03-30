package server;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileNotFoundException;
import java.io.FileReader;

import feedback.Feedback;
import feedback.FeedbackType;

public class Dictionary {

    public String dict_path;
    private JSONObject english_dict;

    // if the path is not provided we can use the default one
    public Dictionary () {
        this.dict_path = "src/dictionary.json";
        processFile();
    }

    public Dictionary (String dict_path) {
        this.dict_path = dict_path;
        processFile();
    }

    private void processFile() {

        try {
            FileReader file = new FileReader(this.dict_path);
            JSONTokener token = new JSONTokener(file);
            this.english_dict = new JSONObject(token);
        } catch (FileNotFoundException e) {
           System.out.println(e.getMessage());
        }

    }

    public JSONObject processString (String str) {
        JSONObject obj = new JSONObject(str);
        return obj;
    }

    // todo: finish this query function
    // find a meaning of a word from dictionary, return either
    // the meaning string or error message (word not found)
    public Feedback query(String word) {

        if (english_dict.has(word)){
            return new Feedback(FeedbackType.SUCCESS, english_dict.getString(word));
        } else {
            return new Feedback(FeedbackType.ERROR,"Word not exist");
        }

    }

    // todo : finish this function
    // add a word to dictionary with meaning while return String could be
    // either success or error message (word already exist)
    public synchronized Feedback add(String word, String meaning){
        return new Feedback(FeedbackType.ERROR, "word already exist");
    }

    // todo : finish this function
    // delete an word from the dictionary and remember to delete from disk
    // return either success or error message (word does not exist)
    public synchronized Feedback delete(String word) {
        return new Feedback(FeedbackType.ERROR, "word does not exist");
    }

}
