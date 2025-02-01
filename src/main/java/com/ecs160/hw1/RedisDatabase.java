package com.ecs160.hw1;

import redis.clients.jedis.Jedis;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class RedisDatabase {
    private final Jedis jedis;

    public RedisDatabase(String host, int port){
        this.jedis = new Jedis(host, port);
        System.out.println("Connected to Redis!");
    }

    /*
        Automatically generate an id (increment it as well)
        jedis.incr(key):
            automatically increments the numeric value for given key
            if key does not exist, it initializes it to 0 and increments it to 1
     */
    private String generateId(String key){
        return String.valueOf(jedis.incr(key));
    }

    // Store posts
    /*
        Jedis stores data as hash key value pairs
        Final example:
            Key: "post:1"
            Fields:
                "author" -> "Hassan Ali"
                "text" -> "Here in my garage"
                "replyCount" -> "0"
     */
    public String storePost(String author, String postDate, String postContent, int replyCount){
        // generate a uniqueID for the post such as 1
        String postId = "post:" + generateId("postId");
        jedis.hset(postId, "author", author);
        jedis.hset(postId, "postDate", postDate);
        jedis.hset(postId, "text", postContent);
        jedis.hset(postId, "replyCount", String.valueOf(replyCount));

        return postId;
    }

    // Store replies and link to post
    public String storeReply(String parentPostId, String author, String text,  String postDate){
        String replyId = "reply:" + generateId("replyId");
        jedis.hset(replyId, "author", author);
        jedis.hset(replyId, "text", text);
        jedis.hset(replyId, "postDate", postDate);
        jedis.hset(replyId, "originalPostId", parentPostId);

        // link reply to post
        // uses a set data structure to join posts with replies
        /*
            example:
                Key: "replies:post:1"
                Set Members:
                    "3", "4"
         */
        jedis.sadd(parentPostId + ":replies", replyId);

        // System.out.println("DEBUG: Before updating replyCount -> " + jedis.smembers(parentPostId + ":replies"));
        // Update replyCount
        long replyCount = jedis.scard(parentPostId + ":replies"); // Get the actual count of replies
        jedis.hset(parentPostId, "replyCount", String.valueOf(replyCount));
//        System.out.println("DEBUG: After updating replyCount -> " + jedis.smembers(parentPostId + ":replies"));
//        System.out.println("DEBUG: replyCount set to -> " + replyCount);

        return replyId;
    }

    public void updateReplyCount(String postId, int replyCount){
        jedis.hset("post:" + postId, "replyCount", String.valueOf(replyCount));
    }

    // Get a post by ID
    public Map<String, String> getPost(String postId){
        return jedis.hgetAll("post:" + postId);
    }

    // Get all replies for a post
    public Set<String> getReplies(String key){
        // The key for retrieving replies is always "<key>:replies"
        // post:1690
        String replyKey = key + ":replies";
        // post:1690:replies

        // If the key doesn't exist, return an empty set
        if (!jedis.exists(replyKey)) {
            return new HashSet<>();
        }

        Set<String> allReplies = new HashSet<>();
        // Fetch all immediate replies from Redis
        Set<String> directReplies = jedis.smembers(replyKey);

        if (directReplies != null && !directReplies.isEmpty()) {
            for (String reply  : directReplies) {
                // Add the immediate reply
                allReplies.add(reply);

                // Recursively collect nested replies
                allReplies.addAll(getReplies(reply));
            }
        }
        return allReplies;
    }

    // Retrieve a reply by replyId
    public Map<String, String> getReply(String replyId){
        return jedis.hgetAll("reply:" + replyId);
    }

    public String getReplyCount(String postId) {
        String replyCountStr = jedis.hget("post:" + postId, "replyCount");
        return (replyCountStr != null) ? replyCountStr : "0";
    }

    public Set<String> getKeys(String pattern) {
        return jedis.keys(pattern);
    }

    public void flushAll(){
        jedis.flushAll();
    }
    // Close connection
    public void close(){
        if (jedis != null){
            jedis.close();
            System.out.println("Redis connection ended...");
        }
    }
}

