package com.thefunone.chatwm.networking;

import java.io.Serializable;

public class ConnectionMsg implements Serializable {
    private static final long serialVersionUID = 7641554368229183353L;
    String username;
    public DataMsgType msgType;
    long epochSeconds;

    ConnectionMsg(DataMsgType msgType, String username) {
        this.msgType = msgType;
        this.username = username;
        epochSeconds = System.currentTimeMillis() / 1000;
    }
    public long getEpochSeconds(){
        return epochSeconds;
    }
    public String getUsername(){
        return username;
    }
}