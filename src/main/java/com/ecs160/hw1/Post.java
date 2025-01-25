package com.ecs160.hw1;

public class Post {
    private String authorName;
    private String postDate;
    private String postContent;
    private int replyCount;

    public Post(String authorName, String postDate, String postContent, int replyCount) {
        this.authorName = authorName;
        this.postDate = postDate;
        this.postContent = postContent;
        this.replyCount = replyCount;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getPostDate() {
        return postDate;
    }

    public void setPostDate(String postDate) {
        this.postDate = postDate;
    }

    public String getPostContent() {
        return postContent;
    }

    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public int getReplyCount() {
        return replyCount;
    }

    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }
}