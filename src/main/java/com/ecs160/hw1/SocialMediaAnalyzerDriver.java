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
        // Initialize default values
        String authorName = "Unknown Author";
        String postDate = "Unknown Date";
        String postContent = "No Content";
        int replyCount = 0;

        Post post = null;

        try {
            // Parse the main post
            if (threadObject.has("post") && threadObject.get("post").isJsonObject()) {
                JsonObject postObject = threadObject.getAsJsonObject("post");

                // Extract post details
                if (postObject.has("author") && postObject.get("author").isJsonObject()) {
                    JsonObject authorObject = postObject.getAsJsonObject("author");
                    if (authorObject.has("displayName")) {
                        authorName = authorObject.get("displayName").getAsString();
                    }
                }

                if (postObject.has("record") && postObject.get("record").isJsonObject()) {
                    JsonObject recordObject = postObject.getAsJsonObject("record");
                    if (recordObject.has("createdAt")) {
                        postDate = recordObject.get("createdAt").getAsString();
                    }
                    if (recordObject.has("text")) {
                        postContent = recordObject.get("text").getAsString();
                    }
                }

                if (threadObject.has("replies") && threadObject.get("replies").isJsonArray()) {
                    JsonArray repliesArray = threadObject.getAsJsonArray("replies");
                    replyCount = repliesArray.size();
                }

                // Create the Post object
                post = new Post(authorName, postDate, postContent, replyCount);
            }

            // Parse replies (if any)
            List<Thread> replies = new ArrayList<>();
            if (threadObject.has("replies") && threadObject.get("replies").isJsonArray()) {
                JsonArray repliesArray = threadObject.getAsJsonArray("replies");

                for (JsonElement replyElement : repliesArray) {
                    if (replyElement.isJsonObject()) {
                        Thread replyThread = processThread(replyElement.getAsJsonObject(), gson);
                        if (replyThread != null) {
                            replies.add(replyThread);
                        }
                    }
                }
            }

            return new Thread(post, replies);

        } catch (Exception e) {
            System.err.println("Error processing thread: " + e.getMessage());
            return null;
        }
    }

    public static void storeReplies(List<Thread> replies, String parentPostId, RedisDatabase redisDb){
        if (replies == null || replies.isEmpty()) return;

        for (Thread replyThread : replies){
            Post replyPost = replyThread.getPost();

            // Store the reply and link to the parent post
            String replyId = redisDb.storeReply(
                    parentPostId,
                    replyPost.getAuthorName(),
                    replyPost.getPostContent()
            );

//            // Update reply count for the parent post (just for safety)
//            redisDb.updateReplyCount(parentPostId, redisDb.getReplies(parentPostId).size());

            // Use recursion to handle nested replies
            storeReplies(replyThread.getReplies(), replyId, redisDb);
        }
    }

    public static void parseDataIntoDatabase(String jsonFile, RedisDatabase redisDb){
        try {
            // Parse Json file into Threads
            InputStreamReader reader = new InputStreamReader(
                    SocialMediaAnalyzerDriver.class.getClassLoader().getResourceAsStream(jsonFile));
            Gson gson = new Gson();

            // Use Gson to first parse the feed array
            var jsonElement = JsonParser.parseReader(reader).getAsJsonObject();

//            System.out.print(jsonElement); // prints everything starting with {"feed":[{"thread":
            if (jsonElement.has("feed") && jsonElement.get("feed").isJsonArray()){
                var feedArray = jsonElement.get("feed").getAsJsonArray();

                for (var threadElement : feedArray){
                    // Parse each thread into an object of type Thread
                    if (!(threadElement.getAsJsonObject().has("thread"))) continue;

                    JsonObject threadObject = threadElement.getAsJsonObject().get("thread").getAsJsonObject();
                    Thread thread = processThread(threadObject, gson);

                    // Debugging: Print post details
//                    Post post = thread.getPost();
//                    System.out.println("Post Content: " + post.getPostContent());
//                    System.out.println("Author Name: " + post.getAuthorName());
//                    System.out.println("Post Date: " + post.getPostDate());
//                    System.out.println("Reply Count: " + post.getReplyCount());
                    // Store Original Post in Redis
                    if (thread != null){
                        Post originalPost = thread.getPost();
                        String parentPostId = redisDb.storePost(
                                originalPost.getAuthorName(),
                                convertTimestamp(originalPost.getPostDate()),
                                originalPost.getPostContent(),
                                originalPost.getReplyCount()
                        );
//                        System.out.println("Loaded post from author: " + originalPost.getAuthorName());
//                        System.out.println("Post Content:\n" + originalPost.getPostContent());

                        // Store Reply
                        storeReplies(thread.getReplies(), parentPostId, redisDb);
//                        System.out.println("Stored replies successfully.");
                    }
                }
            }
        } catch (Exception e){
            System.err.println("Error parsing Json File: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        RedisDatabase redisDb = new RedisDatabase("localhost", 6379);
        try {
            // weighted = true if args[0] == "true"; weighted = false otherwise
            Boolean weighted = Boolean.parseBoolean(args[0]);   // parseBoolean is case insensitive

            String inputFile = "input.json"; // Default file
            if (args.length > 1) {
                inputFile = args[1];
            }

            // Store json data into database
            parseDataIntoDatabase(inputFile, redisDb);

            // Do the required statistics
            SocialMediaAnalyzer analyzer = new SocialMediaAnalyzer(redisDb);
            if (weighted) {
                System.out.println("Weighted total posts: " + analyzer.weightedTotalPosts());
                System.out.println("Weighted average replies per post: " + analyzer.weightedAverageRepliesPerPost());
            } else {
                System.out.println("Total posts: " + analyzer.countPosts());
                System.out.println("Average replies per post: " + analyzer.averageRepliesPerPost());
            }

            for (String postId : analyzer.averageCommentInterval().keySet()) {
                String interval = analyzer.averageCommentInterval().get(postId);
                System.out.println("Post ID: " + postId + ", Avg interval between comments: " + interval);
            }
            
        } catch (Exception e) {
            log.error("Error connecting to Redis: ", e);
        } finally {
            redisDb.close();
        }
    }
}
