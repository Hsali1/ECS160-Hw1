package com.ecs160.hw1;

import java.util.List;

public class Thread {
    // private vars
    private Post post;  // original post
    private List<Post> replies; // list of replies to original post, or replies of replies

    // constructor
    public Thread(Post post, List<Post> replies) {
        this.post = post;
        this.replies = replies;
    }

    // helper fcns
    // get/set post
    public Post getPost() {
        return post;
    }
    public void setPost(Post post) {
        this.post = post;
    }

    // get/set replies
    public List<Post> getReplies() {
        return replies;
    }
    public void setReplies(List<Post> replies) {
        this.replies = replies;
    }
}
