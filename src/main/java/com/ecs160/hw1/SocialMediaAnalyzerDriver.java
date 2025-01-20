package com.ecs160.hw1;



import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.List;
import com.google.gson.JsonDeserializer;
import java.io.InputStreamReader;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;



public class SocialMediaAnalyzerDriver {
    public static void main(String[] args) {


        JsonElement element = JsonParser.parseReader(new InputStreamReader(JsonDeserializer.class.getClassLoader().getResourceAsStream("input.json")));
        System.out.println("it work!");
//        System.out.println(element); prints the entire file i think
        if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
            System.out.println("it work again!");
            JsonArray feedArray = jsonObject.get("feed").getAsJsonArray();
            for (JsonElement feedObject: feedArray) {
                // Check if you have the thread key
                if (feedObject.getAsJsonObject().has("thread")) {
                    // parse the post and any replies (recursively)?
                }
            }
        }
    }
}
