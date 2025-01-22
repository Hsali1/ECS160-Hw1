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
import java.util.Map;
import java.util.Set;



public class SocialMediaAnalyzerDriver {
    private static final Logger log = LoggerFactory.getLogger(SocialMediaAnalyzerDriver.class);

    public static void parseDataIntoDatabase(String jsonFile, RedisDatabase redisDb){
        JsonElement element = JsonParser.parseReader(new InputStreamReader(JsonDeserializer.class.getClassLoader().getResourceAsStream(jsonFile)));
//        System.out.println("it work!");
//        System.out.println(element); prints the entire file I think
        if (element.isJsonObject()) {
            JsonObject jsonObject = element.getAsJsonObject();
//            System.out.println("it work again!");
            JsonArray feedArray = jsonObject.get("feed").getAsJsonArray();
            System.out.println("Feed size: " + feedArray.size()); // Debugging line
            for (JsonElement feedObject: feedArray) {
                // Check if you have the thread key
                // Then access each thread object
                if (feedObject.getAsJsonObject().has("thread")){
                    JsonObject threadObject = feedObject.getAsJsonObject().get("thread").getAsJsonObject();
                    // prints all the contents of each thread object
                    // System.out.println(threadObject);
                    // Access the post object
                    JsonObject postObject = threadObject.getAsJsonObject().get("post").getAsJsonObject();
                    // prints the contents of each post object in each thread object
//                    System.out.println(postObject);
                    String author = postObject.get("author").getAsJsonObject().get("displayName").getAsString();
                    String postContent = postObject.get("record").getAsJsonObject().get("text").getAsString();
                    int replyCount = postObject.get("replyCount").getAsInt();
                    //System.out.println(author + " posted: " + postContent + ", and got: " + replyCount + " replies\n");
                    // Store posts in Redis
                    String postId = redisDb.storePost(author, postContent, replyCount);
                    System.out.println("Stored post ID: " + postId); // Debugging line
                    // Check replies and update replyCount if needed
                    if (threadObject.has("replies") && threadObject.get("replies").isJsonArray()){
                        JsonArray repliesArray = threadObject.get("replies").getAsJsonArray();
                        System.out.println("Reply size for post " + postId + ": " + repliesArray.size());
                        // Update counts (I decided to do this regardless of count mismatch)
                        replyCount = repliesArray.size();
                        redisDb.updateReplyCount(postId, replyCount);

                        for (JsonElement replyElement : repliesArray){
                            JsonObject replyObject = replyElement.getAsJsonObject();
                            String replyAuthor = replyObject.get("author").getAsJsonObject().get("displayName").getAsString();
                            String replyText = replyObject.get("record").getAsJsonObject().get("text").getAsString();

                            // Store reply in database
                            String replyId = redisDb.storeReply(postId, replyAuthor, replyText);
                            System.out.println("Stored reply ID: " + replyId); // Debugging line
                        }
                    }
                }
            }
        } else {
            System.out.println("No JSON data found in the file");
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

            // Test
            // Verify posts
            System.out.println("Stored Posts:");
            Set<String> postKeys = redisDb.getKeys("post:*");
            for (String postKey : postKeys) {
                if (!postKey.equals("postId")) { // Ignore the postId counter key
                    Map<String, String> post = redisDb.getPost(postKey.split(":")[1]);
                    System.out.println("Key: " + postKey);
                    post.forEach((key, value) -> System.out.println(key + ": " + value));
                }
            }
            // Do the required statistics

        } catch (Exception e) {
            log.error("Error connecting to Redis: ", e);
        } finally {
            redisDb.close();
        }
    }
}
