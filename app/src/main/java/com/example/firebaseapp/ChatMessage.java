package com.example.firebaseapp;

/**
 * Created by Anand on 25/11/2016.
 */

public class ChatMessage {

    private String name;
    private String msg;
    private String photoUrl;

    public ChatMessage() {
    }

    public ChatMessage(String name, String msg, String photoUrl) {
        this.setName(name);
        this.setMsg(msg);
        this.setPhotoUrl(photoUrl);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
