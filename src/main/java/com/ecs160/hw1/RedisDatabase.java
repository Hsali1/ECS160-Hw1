package com.ecs160.hw1;

import redis.clients.jedis.Jedis;
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
    public String storeReply(String parentPostId, String author, String text){
        String replyId = "reply:" + generateId("replyId");
        jedis.hset(replyId, "author", author);
        jedis.hset(replyId, "text", text);
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
        return replyId;
    }

    public void updateReplyCount(String postId, int replyCount){
        jedis.hset("post:" + postId, "replyCount", String.valueOf(replyCount));
    }

    // Get a post by ID
    public Map<String, String> getPost(String postId){
        return jedis.hgetAll("Post:" + postId);
    }

    // Get all replies for a post
    public Set<String> getReplies(String postId){
        return jedis.smembers("replies:post:" + postId);
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

    // Close connection
    public void close(){
        if (jedis != null){
            jedis.close();
            System.out.println("Redis connection ended...");
        }
    }
}

