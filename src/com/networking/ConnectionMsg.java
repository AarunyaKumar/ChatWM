package com.networking;

import java.io.Serializable;
import java.time.Instant;

public class ConnectionMsg implements Serializable {
    String username;
    public DataMsgType msgType;
    long epochSeconds;

    ConnectionMsg(DataMsgType msgType, String username) {
        this.msgType = msgType;
        this.username = username;
        epochSeconds = Instant.now().getEpochSecond();
    }
    public long getEpochSeconds(){
        return epochSeconds;
    }
    public String getUsername(){
        return username;
    }
}
