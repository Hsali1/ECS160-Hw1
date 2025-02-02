package com.ecs160.hw1;

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/*
    Your application should compute the following basic statistics for the provided
    posts and replies. These statistics are:
        - the total number of posts
        - the average number of replies per post
        - average interval between comments (for posts which have comments) in a HH:MM:SS format
    For purposes of calculation, you should consider the post itself as a comment.
    If for a post, the number of actual replies and the value of the replyCount Json field
        are different, please use the number of actual replies.

    Depending on an option provided on the command line (weighted = true|false),
        you will either compute a simple average,
        or a weighted average that depends on the length of the post or comments for the
            first two statistics (total number of posts, average number of replies per post).

    Weighted average: The goal of the weighted average computation is to provide more
    weightage for longer posts.

    The formula for the weight of a post:
    Weight = (1 + (NumOfWordsInPost / NumOfWordsInLongestPost))

    Therefore, to compute the average number of replies, we will first count NumOfWordsInLongestPost.
    Then, for each reply to each post, we will compute the Weight of that reply, using the above formula.

    Then, we will compute the total number of posts as follows:
    WeightedTotalPosts = (Summation from n=1 to N (Weight_n))
    Here, N is the total number of posts, and Weight_n is the weight of the post n.

    And, we will compute the average number of replies, as follows:
    WeightedAvgNumReplies = (Summation from n=1 to N (Summation from m=1 to M (Weight_m)) / N
    N is the total number of posts, M is the total replies of post n. Weight_m is the weight of the reply m.
    */

public class SocialMediaAnalyzer {

    // initialize RedisDatabase object
    private final RedisDatabase redisDb;

    private final Map<String, Integer> numOfReplies;

    private final int longestPost;

    public SocialMediaAnalyzer(RedisDatabase redisDb){
        this.redisDb = redisDb;
        this.numOfReplies = new HashMap<>();
        this.longestPost = longestPost();
    }

    private LocalDateTime parseDate(String date){
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    // return number of posts in db
    public int countPosts() { // int or long?
        // all posts have a key starting with "posts:"
        Set<String> postKeys = redisDb.getKeys("post:*");
        int counter = 0;
        for (String postKey : postKeys) {
            if (postKey.endsWith(":replies")) {
                continue;
            }
            counter++;
        }
        return counter;
    }

    public void countReplies() {
        Set<String> postKeys = redisDb.getKeys("post:*");

        // for each post, retrieve replyCount
        for (String postKey : postKeys) {
            if (postKey.endsWith(":replies")) {
                continue;
            }
            String postId = postKey.split(":")[1];  // post:postId
            // 0    1
            String replyCount = redisDb.getReplyCount(postId);

            numOfReplies.put(postId, Integer.parseInt(replyCount));
        }
    }

    // average replies per post
    public double averageRepliesPerPost() {
        countReplies();
        int totalReplies = 0;
        int totalPosts = numOfReplies.size(); // total posts will be the size of the map

        for (int replyCount : numOfReplies.values()) {
            totalReplies += replyCount;
        }

        if (totalPosts != 0)
            return (double) totalReplies / totalPosts;
        else
            return 0;
    }

    // Average interval between comments
    public String averageCommentInterval() {
        List<Long> allIntervals = new ArrayList<>();
        Set<String> postKeys = redisDb.getKeys("post:*");
        try {
            for (String postKey : postKeys) {
                if (postKey.endsWith(":replies")) {
                    continue;
                }

                String key = postKey.split(":")[1];
                Set<String> replyKeys = redisDb.getReplies(postKey);
                if (replyKeys == null || replyKeys.isEmpty()) {
                    continue;
                }

                List<LocalDateTime> postDates = new ArrayList<>();

                // Fetch post data
                Map<String, String> post = redisDb.getPost(key);
                if (post == null || !post.containsKey("postDate")) {
                    continue;
                }

                String postDate = post.get("postDate");
                if (postDate == null || postDate.isEmpty()) {
                    continue;
                }

                // Add post date
                try {
                    postDates.add(parseDate(postDate));
                } catch (Exception e) {
                    continue;
                }

                // Fetch and add reply timestamps
                for (String replyKey : replyKeys) {
//                    System.out.println("replykey-->" + replyKey);
                    String replyK = replyKey.split(":")[1];
                    Map<String, String> reply = redisDb.getReply(replyK);
                    if (reply == null || !reply.containsKey("postDate")) {
                        continue;
                    }

                    String replyDate = reply.get("postDate");
                    if (replyDate == null || replyDate.isEmpty()) {
                        continue;
                    }

                    try {
                        postDates.add(parseDate(replyDate));
                    } catch (Exception e) {
//                        System.out.println("ERROR: Invalid date format for reply -> " + replyKey + " | Date: " + replyDate);
                        continue;
                    }
                }

                Collections.sort(postDates);

                // Calculate intervals
                List<Long> intervals = new ArrayList<>();
                for (int i = 0; i < postDates.size() - 1; i++) {
                    long interval = java.time.Duration.between(postDates.get(i), postDates.get(i + 1)).getSeconds();
                    intervals.add(interval);
                }

                if (!intervals.isEmpty()) {
                    long sum = 0;
                    for (long interval : intervals) {
                        sum += interval;
                    }
                    long avgForPost = sum / intervals.size();
                    allIntervals.add(avgForPost);
                }
            }

            // Compute final average interval
            if (allIntervals.isEmpty()) {
                return "No valid interval";
            }

            long totalSum = 0;
            for (long interval : allIntervals) {
                totalSum += interval;
            }
            long finalAvg = totalSum / allIntervals.size();

            long hours = finalAvg / 3600;
            long minutes = (finalAvg % 3600) / 60;
            long seconds = finalAvg % 60;

            return String.format("%02d:%02d:%02d", hours, minutes, seconds);

        } catch (Exception e) {
            System.err.println("ERROR: Exception in averageCommentInterval()");
            e.printStackTrace();
            return "Error occurred";
        }
    }


    private int countWords(String postContents) {
        if (postContents == null || postContents.isEmpty())
            return 0;

        StringTokenizer tokenizer = new StringTokenizer(postContents);
        return tokenizer.countTokens();
    }

    private int longestPost() {
        Set<String> postKeys = redisDb.getKeys("post:*");
        int longestCount = 0;

        for (String postKey : postKeys) {
            String postId = postKey.split(":")[1]; // post:postId
            // retrieve each post
            Map<String, String> post = redisDb.getPost(postId);
            // retrieve post content
            String postContent = post.get("text");

            int count = countWords(postContent);
            if (count > longestCount) {
                longestCount = count;
            }
        }
        return longestCount;
    }

    /* Weight = (1 + (NumOfWordsInPost / NumOfWordsInLongestPost)) */
    public double postWeight(String postId) {
        if (longestPost  == 0)
            return 1;
        Map<String, String> post = redisDb.getPost(postId);
        if (post == null || !post.containsKey("text")){
            return 1;
        }
        // retrieve post content
        String postContent = post.get("text");
        // count words in post
        int numOfWordsInPost = countWords(postContent);
        // calculate weight
        return (double) (1 + (numOfWordsInPost / longestPost));
    }
    public double replyWeight(String replyId) {
        if (longestPost  == 0)
            return 1;
        Map<String, String> reply = redisDb.getReply(replyId);
        if (reply == null || !reply.containsKey("text")){
            return 1;
        }
        // retrieve post content
        String replyContent = reply.get("text");
        // count words in post
        int numOfWordsInPost = countWords(replyContent);
        // calculate weight
        return (double) (1 + (numOfWordsInPost / longestPost ));
    }


    /* WeightedTotalPosts = (Summation from n=1 to N (Weight_n))
    Here, N is the total number of posts, and Weight_n is the weight of the post n. */
    public double weightedTotalPosts() {
        double totalWeight = 0;
        Set<String> postKeys = redisDb.getKeys("post:*");
        for (String postKey : postKeys) {
            if (postKey.endsWith(":replies")) continue;
            String postId = postKey.split(":")[1];
            totalWeight += postWeight(postId);
        }
        return totalWeight;
    }

    /* WeightedAvgNumReplies = (Summation from n=1 to N (Summation from m=1 to M (Weight_m)) / N
    N is the total number of posts, M is the total replies of post n. Weight_m is the weight of the reply m. */
    public double weightedAverageRepliesPerPost() {
        double totalWeight = 0;
        Set<String> postKeys = redisDb.getKeys("post:*");
        for (String postKey : postKeys) {
            // String postId = postKey.split(":")[1];
            if (postKey.endsWith(":replies")) {
                continue;
            }
            Set<String> replyKeys = redisDb.getReplies(postKey);
            for (String replyKey : replyKeys) {
                if (!replyKey.startsWith("reply:")) continue;
                // retrieve each reply
                Map<String, String> reply = redisDb.getReply(replyKey);
                // retrieve post content
                totalWeight += replyWeight(replyKey);
            }
        }
        return totalWeight / postKeys.size();
    }
}
