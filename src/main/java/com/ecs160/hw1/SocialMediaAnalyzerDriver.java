package com.ecs160.hw1;



import java.io.FileNotFoundException;
import com.google.gson.JsonDeserializer;
import java.io.InputStreamReader;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.List;



public class SocialMediaAnalyzerDriver {
    private static final Logger log = LoggerFactory.getLogger(SocialMediaAnalyzerDriver.class);

    public static String convertTimestamp(String timestamp) {
        try {
            // Remove the 'Z' and parse the timestamp
            LocalDateTime dateTime = LocalDateTime.parse(
                    timestamp.replace("Z", ""),
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
            );

            // Format into a readable string
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return dateTime.format(outputFormatter);
        } catch (Exception e) {
            return "Invalid timestamp: " + timestamp;
        }
    }

    public static Thread processThread(JsonObject threadObject, Gson gson){
        // Parse the main post
        JsonObject postObject = threadObject.getAsJsonObject("post");

        // Initialize variables with default values
        String authorName = "Unknown Author";
        String postDate = "Unknown Date";
        String postContent = "No Content";
        int replyCount = 0;

        try {
            // Check if "author" exists and get "displayName"
            if (postObject.has("author") && postObject.get("author").getAsJsonObject().has("displayName")) {
                authorName = postObject.getAsJsonObject("author").get("displayName").getAsString();
            }

            // Check if "record" exists and contains "createdAt" and "text"
            if (postObject.has("record") && postObject.get("record").isJsonObject()) {
                JsonObject recordObject = postObject.getAsJsonObject("record");
                if (recordObject.has("createdAt")) {
                    postDate = recordObject.get("createdAt").getAsString();
                }
                if (recordObject.has("text")) {
                    postContent = recordObject.get("text").getAsString();
                }
            }

            // Use the actual array size for reply count
            if (threadObject.has("replies") && threadObject.get("replies").isJsonArray()) {
                replyCount = threadObject.getAsJsonArray("replies").size();
            }

        } catch (Exception e) {
            System.err.println("Error parsing post: " + e.getMessage());
        }

        // Create the Post object
        Post post = new Post(authorName, postDate, postContent, replyCount);

        // Parse the replies (if any)
        List<Thread> replies = null;
        if (threadObject.has("replies") && threadObject.get("replies").isJsonArray()){
            JsonArray repliesArray = threadObject.getAsJsonArray("replies");
            replies = new ArrayList<>();

            for (var replyElement : repliesArray){
                JsonObject replyThreadObject = replyElement.getAsJsonObject();
                // Parse each reply recursively as a Thread
                Thread replyThread = processThread(replyThreadObject, gson);
                replies.add(replyThread);
            }
        }

        return new Thread(post, replies);
    }

    public static void parseDataIntoDatabase(String jsonFile, RedisDatabase redisDb){
        try {
            // Parse Json file into Threads
            InputStreamReader reader = new InputStreamReader(
                    SocialMediaAnalyzerDriver.class.getClassLoader().getResourceAsStream(jsonFile));
            Gson gson = new Gson();

            // Use Gson to first parse the feed array
            var jsonElement = JsonParser.parseReader(reader);

//            System.out.print(jsonElement); // prints everything starting with {"feed":[{"thread":
            if (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().has("feed")){
                var feedArray = jsonElement.getAsJsonObject().get("feed").getAsJsonArray();

                for (var threadElement : feedArray){
                    // Parse each thread into an object of type Thread
                    JsonObject threadObject = threadElement.getAsJsonObject().get("thread").getAsJsonObject();
                    Thread thread = processThread(threadObject, gson);

                    // Debugging: Print post details
//                    Post post = thread.getPost();
//                    System.out.println("Post Content: " + post.getPostContent());
//                    System.out.println("Author Name: " + post.getAuthorName());
//                    System.out.println("Post Date: " + post.getPostDate());
//                    System.out.println("Reply Count: " + post.getReplyCount());
                    // Store Original Post in Redis
                    Post originalPost = thread.getPost();
                    String authorName = originalPost.getAuthorName();
                    String postDate = originalPost.getPostDate();
                    String postContent = originalPost.getPostContent();
                    int replyCount = originalPost.getReplyCount();
                    String postId = redisDb.storePost(authorName, postDate, postContent, replyCount);
                    System.out.println("Loaded post from author: " + authorName);
                    System.out.println("Post Content:\n" + postContent);
                }
            }
        } catch (Exception e){
            System.err.println("Error parsing Json File: " + e.getMessage());
        }
    }

    public static void main(String[] args) {

        RedisDatabase redisDb = new RedisDatabase("localhost", 6379);
        try {

            String inputFile = "input.json"; // Default file
            if (args.length > 0) {
                inputFile = args[0];
            }

            // Store json data into database
            parseDataIntoDatabase(inputFile, redisDb);

            // Do the required statistics

        } catch (Exception e) {
            log.error("Error connecting to Redis: ", e);
        } finally {
            redisDb.close();
        }
    }
}
