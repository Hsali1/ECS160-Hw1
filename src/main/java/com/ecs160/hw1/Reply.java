package com.ecs160.hw1;

public class Reply extends Post {
    // private var to link to (relative) parent -- replies to main post, or replies of replies
    private Post parentPost;

    // constructor -- super creates an object thats both
    public Reply(String authorName, String postDate, String postContent, int replyCount) {
        super(authorName, postDate, postContent, replyCount);
    }

    // get/set parent post
    public Post getParentPost() {
        return parentPost;
    }
    public void setParentPost(Post parentPost) {
        this.parentPost = parentPost;
    }
}
