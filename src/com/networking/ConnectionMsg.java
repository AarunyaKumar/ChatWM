package com.networking;

import java.io.Serializable;
import java.time.Instant;

enum DataMsgType{
    JOINED,
    LEFT,
    UID_GIVER
}

public class ConnectionMsg implements Serializable {
    String username;
    DataMsgType msgType;
    long epochSeconds;

    ConnectionMsg(DataMsgType msgType, String username) {
        this.msgType = msgType;
        this.username = username;
        epochSeconds = Instant.now().getEpochSecond();
    }
    long getEpochSeconds(){
        return epochSeconds;
    }
    String getUsername(){
        return username;
    }
}
