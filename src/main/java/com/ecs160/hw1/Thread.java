package com.ecs160.hw1;

import java.util.List;

public class Thread {
    // private vars
    private Post post;  // original post
    private List<Thread> replies; // Each is a thread object

    // constructor
    public Thread(Post post, List<Thread> replies) {
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
    public List<Thread> getReplies() {
        return replies;
    }
    public void setReplies(List<Thread> replies) {
        this.replies = replies;
    }
}
