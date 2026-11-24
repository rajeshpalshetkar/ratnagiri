package com.app.bloggernewsapp400.callback;

import com.app.bloggernewsapp400.model.Author;
import com.app.bloggernewsapp400.model.Blog;
import com.app.bloggernewsapp400.model.Replies;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CallbackPostDetail implements Serializable {

    public String kind;
    public String id;
    public Blog blog = null;
    public String published;
    public String updated;
    public String url;
    public String selflink;
    public String title;
    public String content;
    public Author author = null;
    public Replies replies = null;
    public List<String> labels = new ArrayList<>();
    public String etag;

}
