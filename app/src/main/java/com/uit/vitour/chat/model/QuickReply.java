package com.uit.vitour.chat.model;

public class QuickReply {
    private String text;
    private String action; // optional action identifier if needed

    public QuickReply(String text, String action) {
        this.text = text;
        this.action = action;
    }

    public QuickReply(String text) {
        this.text = text;
        this.action = "";
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
