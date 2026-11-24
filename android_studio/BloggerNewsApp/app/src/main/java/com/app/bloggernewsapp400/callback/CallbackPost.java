package com.app.bloggernewsapp400.callback;

import com.app.bloggernewsapp400.model.Post;

import java.util.ArrayList;
import java.util.List;

public class CallbackPost {

    public String kind;
    public String nextPageToken;
    public List<Post> items = new ArrayList<>();
    public String etag;

}
