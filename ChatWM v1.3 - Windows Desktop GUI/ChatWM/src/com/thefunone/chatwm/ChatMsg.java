package com.thefunone.chatwm;

import java.io.Serializable;


public class ChatMsg implements Serializable {
    private static final long serialVersionUID = 881748675381070757L;
    public ChatMsgType chatMsgType;
    public String username, message;
    public long epochSeconds;
    public byte[] fileData;

    public ChatMsg(ChatMsgType chatMsgType, String username, String message) {
        this.chatMsgType = chatMsgType;
        this.username = username;
        this.message = message;
        epochSeconds = System.currentTimeMillis() / 1000;
    }
    public ChatMsg(ChatMsgType chatMsgType, String username, String message, byte[] fileData) {
        this(chatMsgType, username, message);
        this.fileData = fileData;
    }
    public long getEpochSeconds(){
        return epochSeconds;
    }
    public String getUsername(){
        return username;
    }
}
