//package com.ecs160.hw1;
//
//
//
//import java.util.ArrayList;
//import java.util.List;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//import java.util.Map;
//import java.util.Set;
//
//public class BasicAnalyzerTest {
//    private RedisDatabase redisDb;
//    private SocialMediaAnalyzer analyzer;
//
//    @BeforeEach
//    public void setUp(){
//        redisDb = new RedisDatabase("localhost", 6379);
//        analyzer = new SocialMediaAnalyzer(redisDb);
//        //redisDb.flushAll();
//    }
//
//    @AfterEach
//    public void tearDown(){
//        redisDb.close();
//    }
//
//    @Test
//    public void testStorePost(){
//        String postId = redisDb.storePost("TestUser1", "2025-1-1 1:00:00", "Test post content", 0);
//        Map<String, String> post = redisDb.getPost(postId);
//
//        assertNotNull(post);
//        assertEquals("TestUser1", post.get("author"));
//        assertEquals("Test post content", post.get("text"));
//        assertEquals("0", post.get("replyCount"));
//    }
//
//    @Test
//    public void testStoreReply(){
//        String postId = redisDb.storePost("TestUser1", "2025-1-1 1:00:00", "Test post content", 0);
//        String replyId = redisDb.storeReply(postId, "User2", "2025-1-1 1:00:00","This is a reply");
//
//        Map<String, String> reply = redisDb.getReply(replyId);
//        assertNotNull(reply);
//        assertEquals("User2", reply.get("author"));
//        assertEquals("This is a reply", reply.get("text"));
//    }
//
//    @Test
//    public void testReplyCount() {
//        String postId = redisDb.storePost("TestUser1", "2025-1-1 1:00:00", "Test post content", 0);
//        redisDb.storeReply(postId, "User2", "First reply");
//        redisDb.storeReply(postId, "User3", "Second reply");
//
//        String replyCount = redisDb.getReplyCount(postId);
//        assertEquals("2", replyCount);
//    }
//}
