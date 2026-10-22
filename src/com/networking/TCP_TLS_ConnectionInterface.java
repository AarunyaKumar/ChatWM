package com.networking;

public interface TCP_TLS_ConnectionInterface {
    <UserMsg> void gotData(UserMsg userMsg);

    default void connectionEstablish(){}

    default void userJoined(ConnectionMsg connectionMsg){}
    default void userLeft(ConnectionMsg connectionMsg){}

    default void failedToConnect(String information){}
    default void somethingWentWrong(String information){}

    default void failedToSearchNewClient(){}
}
