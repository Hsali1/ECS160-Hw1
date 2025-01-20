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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;



public class SocialMediaAnalyzerDriver {
    private static final Logger log = LoggerFactory.getLogger(SocialMediaAnalyzerDriver.class);

    public static void parseDataIntoDatabase(String jsonFile, Jedis jedis){
        JsonElement element = JsonParser.parseReader(new InputStreamReader(JsonDeserializer.class.getClassLoader().getResourceAsStream(jsonFile)));
//        System.out.println("it work!");
//        System.out.println(element); prints the entire file I think
        if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
//            System.out.println("it work again!");
            JsonArray feedArray = jsonObject.get("feed").getAsJsonArray();
            for (JsonElement feedObject: feedArray) {
                // Check if you have the thread key
                if (feedObject.getAsJsonObject().has("thread")) {
                    // parse the post and any replies (recursively)?
                    // prints the entire thread key
//                    System.out.println(feedObject.getAsJsonObject());
                }
            }
        }
    }

    public static void main(String[] args) {

        try (Jedis jedis = new Jedis("localhost", 6379)) {

            System.out.println("Connected to Redis!");

            String inputFile = "input.json"; // Default file
            if (args.length > 0) {
                inputFile = args[0];
            }

            // Store json data into database
            parseDataIntoDatabase(inputFile, jedis);

            // Do the required statistics

        } catch (Exception e) {
            log.error("Error connecting to Redis: ", e);
        }
    }
}
