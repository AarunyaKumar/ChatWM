package com.thefunone.chatwm;

import com.thefunone.chatwm.networking.*;

import javax.swing.*;

public class ChatManager implements TCP_TLS_ConnectionInterface {
    ChatWindow chatWindow;
    TCP_TLS_Connection connection;

    ChatManager(ChatWindow chatWindow, String username, String ipAddress, int portNum, String keystorePath, String password){
        this.chatWindow = chatWindow;
        connection = new TCP_TLS_Connection(username, ipAddress, portNum, this, keystorePath, password.toCharArray(), new String[]{"com.thefunone.chatwm.ChatMsg", "com.thefunone.chatwm.ChatMsgType", "com.thefunone.chatwm.networking.*"});
        connection.startConnection();
    }



    @Override
    public <UserMsg> void gotData(UserMsg userMsg) {
        if(userMsg instanceof ChatMsg chatMsg){
            if(chatMsg.chatMsgType== ChatMsgType.SIMPLE_CHAT){
                chatWindow.addMessage(DisplayMessageType.SIMPLE_CHAT, chatMsg.getUsername(), chatMsg.message, null);
            }else if(chatMsg.chatMsgType==ChatMsgType.ALERT){
                chatWindow.addMessage(DisplayMessageType.ALERT_CHAT, chatMsg.getUsername(), chatMsg.message, null);
            }else if(chatMsg.chatMsgType==ChatMsgType.FILE){
                chatWindow.addMessage(DisplayMessageType.FILE, chatMsg.getUsername(), chatMsg.message, chatMsg.fileData);
            }
        }
    }

    @Override
    public void connectionEstablished() {
        chatWindow.chatWM.connectedSuccessfully();
        chatWindow.chatWM.setUpWindow = null;
    }

    @Override
    public void userJoined(ConnectionMsg connectionMsg) {
        chatWindow.updateStatus(connection.getUserID(), connection.isServer(), connection.getNumberOfUsers());
        chatWindow.addMessage(DisplayMessageType.JOINED, connectionMsg.getUsername(), "", null);
    }

    @Override
    public void userLeft(ConnectionMsg connectionMsg) {
        chatWindow.updateStatus(connection.getUserID(), connection.isServer(), connection.getNumberOfUsers());
        chatWindow.addMessage(DisplayMessageType.LEFT, connectionMsg.getUsername(), "", null);
    }

    @Override
    public void failedToConnect(String information) {
        if(chatWindow.chatWM.setUpWindow!=null){
            chatWindow.chatWM.failedToConnect(information);
        }else {
            int response = JOptionPane.showConfirmDialog(chatWindow.frame, "Connection Lost, Re-join the server?");
            if(response==JOptionPane.YES_OPTION){
                connection.startConnection();
            }else{
                chatWindow.closeChatWindow();
            }
        }
    }

    @Override
    public void somethingWentWrong(String information) {
        chatWindow.updateStatus(connection.getUserID(), connection.isServer(), connection.getNumberOfUsers());
        chatWindow.showInfoLog(information);
    }

    @Override
    public void failedToSearchNewClient() {
        chatWindow.updateStatus(connection.getUserID(), connection.isServer(), connection.getNumberOfUsers());
        chatWindow.showInfoLog("Error: Failed to search for new Client");
    }
}
