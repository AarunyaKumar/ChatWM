package com.thefunone.chatwm;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.thefunone.chatwm.networking.ConnectionMsg;
import com.thefunone.chatwm.networking.TCP_TLS_Connection;
import com.thefunone.chatwm.networking.TCP_TLS_ConnectionInterface;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

public class ChatManager implements TCP_TLS_ConnectionInterface {

    MainActivity mainActivity;
    ChatActivity chatActivity;

    TCP_TLS_Connection connection;
    List<Object> leftMsg, userJoin, userLeft;
    int numberOfTimeToReconnect;
    String keystorePath;
    char[] password;

    ChatManager(MainActivity mainActivity, String username, String ipAddress, int portNum, String keystorePath, String password){
        this.mainActivity = mainActivity;
        leftMsg = new CopyOnWriteArrayList<>();
        userJoin = new CopyOnWriteArrayList<>();
        userLeft = new CopyOnWriteArrayList<>();
        numberOfTimeToReconnect = 1;
        this.keystorePath = keystorePath;
        this.password = password.toCharArray();
        connection = new TCP_TLS_Connection(username, ipAddress, portNum, this, keystorePath, password.toCharArray(), new String[]{"com.thefunone.chatwm.ChatMsg", "com.thefunone.chatwm.ChatMsgType", "com.thefunone.chatwm.networking.*"});
        startConnection();
    }

    private void startConnection(){
        new Thread(() -> {
            if ("defaultKeyStore.p12".equals(keystorePath)) {
                try {
                    connection.startConnection(createSSLContext(mainActivity, password));
                } catch (Exception e) {
                    mainActivity.failedToConnect("File or password is incorrect! " + e.getMessage());
                }
            } else {
                connection.startConnection(null);
            }
        }).start();
    }
    private SSLContext createSSLContext(Context context, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = context.getAssets().open("defaultKeyStore.p12")) {
            keyStore.load(is, password);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, password);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());

        return sslContext;
    }



    public synchronized void setChatActivity(ChatActivity chatActivity) {
        this.chatActivity = chatActivity;
        if (chatActivity != null) {
            flushPendingEvents();
        }
    }

    public synchronized void flushPendingEvents() {
        if (chatActivity == null) return;

        while (!userJoin.isEmpty()) {
            Object obj = userJoin.remove(0);
            if (obj instanceof ConnectionMsg) {
                userJoined((ConnectionMsg) obj);
            }
        }

        while (!userLeft.isEmpty()) {
            Object obj = userLeft.remove(0);
            if (obj instanceof ConnectionMsg) {
                userLeft((ConnectionMsg) obj);
            }
        }

        while (!leftMsg.isEmpty()) {
            Object obj = leftMsg.remove(0);
            gotData(obj);
        }
    }

    @Override
    public synchronized <UserMsg> void gotData(UserMsg userMsg) {
        if (chatActivity == null) {
            leftMsg.add(userMsg);
            return;
        }
        if (userMsg instanceof ChatMsg) {
            ChatMsg chatMsg = (ChatMsg) userMsg;
            if (chatMsg.chatMsgType == ChatMsgType.SIMPLE_CHAT) {
                chatActivity.addMessage(DisplayMessageType.SIMPLE_CHAT, chatMsg.getUsername(), chatMsg.message, null, chatMsg.epochSeconds);
            } else if (chatMsg.chatMsgType == ChatMsgType.ALERT) {
                chatActivity.addMessage(DisplayMessageType.ALERT_CHAT, chatMsg.getUsername(), chatMsg.message, null, chatMsg.epochSeconds);
            } else if (chatMsg.chatMsgType == ChatMsgType.FILE) {
                chatActivity.addMessage(DisplayMessageType.FILE, chatMsg.getUsername(), chatMsg.message, chatMsg.fileData, chatMsg.epochSeconds);
            }
        }
    }

    @Override
    public void connectionEstablished() {
        mainActivity.connectionEstablished();
    }

    @Override
    public synchronized void userJoined(ConnectionMsg connectionMsg) {
        if (chatActivity == null) {
            userJoin.add(connectionMsg);
            return;
        }
        chatActivity.updateStatus(connection.getUserID(), connection.isServer(), connection.getNumberOfUsers());
        chatActivity.addMessage(DisplayMessageType.JOINED, connectionMsg.getUsername(), "", null, connectionMsg.getEpochSeconds());
    }

    @Override
    public synchronized void userLeft(ConnectionMsg connectionMsg) {
        if (chatActivity == null) {
            userLeft.add(connectionMsg);
            return;
        }
        chatActivity.updateStatus(connection.getUserID(), connection.isServer(), connection.getNumberOfUsers());
        chatActivity.addMessage(DisplayMessageType.LEFT, connectionMsg.getUsername(), "", null, connectionMsg.getEpochSeconds());
    }


    @Override
    public void failedToConnect(String information) {
        if(chatActivity!=null){
            // mainActivity.failedToConnect(information);
            new AlertDialog.Builder(chatActivity)
                    .setTitle("Connection Lost")
                    .setMessage("Re-join the server?")
                    .setPositiveButton("Yes", (dialog, which) -> startConnection())
                    .setNegativeButton("No", (dialog, which) -> chatActivity.exitChat())
                    .setCancelable(false) // Prevents closing the dialog by tapping outside
                    .show();
        }else {
            if(numberOfTimeToReconnect>0) {
                numberOfTimeToReconnect--;
                mainActivity.runOnUiThread(() -> Toast.makeText(mainActivity, "Failed to Connect. Reconnecting to server. Error: " + information, Toast.LENGTH_LONG).show());
                connection.close();
                new Handler(Looper.getMainLooper()).postDelayed(this::startConnection, 5000);
            }else{
                mainActivity.failedToConnect("Failed to connect! "+information);
                connection.close();
            }

        }
    }

    @Override
    public void somethingWentWrong(String information) {
        if (chatActivity != null) {
            chatActivity.updateStatus(connection.getUserID(), connection.isServer(), connection.getNumberOfUsers());
            chatActivity.showInfomation(information);
        }
    }

    @Override
    public void failedToSearchNewClient() {
        if(chatActivity!=null) {
            chatActivity.updateStatus(connection.getUserID(), connection.isServer(), connection.getNumberOfUsers());
            chatActivity.showInfomation("Error: Failed to search for new Client");
        }
    }
}
