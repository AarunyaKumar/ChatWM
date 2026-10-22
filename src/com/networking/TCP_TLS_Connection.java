package com.networking;

import javax.net.ssl.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TCP_TLS_Connection {
    TCP_TLS_ConnectionInterface connectionInterface;

    boolean isConnected = false, isServer = false;
    String username;
    int UID, numOfUser;

    SSLSocket socket;
    ObjectOutputStream outputStream;
    ExecutorService executorService;

    SSLServerSocket serverSocket;
    List<SSLSocket> connectionsSocket;
    List<ObjectOutputStream> connectionsOutputStream;

    public String ipAddress;
    public int portNum;
    public String information = "";
    public List<ConnectionMsg> connectionsMsg = new CopyOnWriteArrayList<>();

    public TCP_TLS_Connection(String username, String ipAddress, int portNum, TCP_TLS_ConnectionInterface connectionInterface){
        this.username = username;
        this.ipAddress = ipAddress;
        this.portNum = portNum;
        this.connectionInterface = connectionInterface;
    }
    private SSLContext createFlexibleSSLContext() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            // Initialize the SSLContext with our custom trust rules
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext;
        } catch (Exception e) {
            System.err.println("Failed to initialize custom secure context, falling back to default.");
            return null;
        }
    }
    public final void startConnection(){
        try{
            socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket(ipAddress, portNum);
            socket.setEnabledProtocols(new String[]{"TLSv1.3"});
            socket.setEnabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"});
            socket.startHandshake();
            portNum = socket.getLocalPort();
            ipAddress = InetAddress.getLocalHost().getHostAddress();
            try{
                executorService = Executors.newFixedThreadPool(1);
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                sendData(new ConnectionMsg(DataMsgType.JOINED, username));
                executorService.execute(() -> {
                    try {
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        do {
                            Object obj = ois.readObject();
                            if(obj instanceof ConnectionMsg){
                                ConnectionMsg connectionMsg = (ConnectionMsg) obj;
                                connectionsMsg.add(connectionMsg);
                                switch (connectionMsg.msgType){
                                    case JOINED:
                                        connectionInterface.userJoined(connectionMsg);
                                        numOfUser++;
                                        break;
                                    case LEFT:
                                        connectionInterface.userLeft(connectionMsg);
                                        numOfUser--;
                                        break;
                                    case UID_GIVER:
                                        UID = Integer.parseInt(connectionMsg.username);
                                        break;
                                }
                            }else{
                                connectionInterface.gotData(obj);
                            }
                        } while (isConnected);
                    } catch (IOException | ClassNotFoundException e) { // EOFException
                        if(isConnected) {
                            isConnected = false;
                            connectionInterface.failedToConnect(information=(e instanceof IOException) ? "Server left! Try to reconnect.": "Received Corrupted Message");
                        }
                    }
                });
                isConnected = true;
                isServer = false;
                numOfUser = 2;
                executorService.shutdown();
                connectionInterface.connectionEstablish();
            }catch (RuntimeException e){
                connectionInterface.somethingWentWrong(information="Something went wrong in threading. Please restart your application");
            }
        }catch(SSLException e){
            connectionInterface.failedToConnect(information="It's not our server!");
        }catch (ConnectException e){
            startConnectionAsServer();
        } catch(IOException e){
            connectionInterface.failedToConnect(information="Failed to connect. Please try again."); ; // Some general exception ask user to re-establish connection
        }
    }
    private void startConnectionAsServer(){
        try {
            serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(portNum);
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.3"});
            serverSocket.setEnabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"});
            portNum = serverSocket.getLocalPort();
            ipAddress = InetAddress.getLocalHost().getHostAddress();
            connectionsSocket = new CopyOnWriteArrayList<>();
            connectionsOutputStream = new CopyOnWriteArrayList<>();

            isConnected = true;
            isServer = true;
            UID = 1;
            numOfUser = 1;

            executorService = Executors.newCachedThreadPool();
            startNewClientListening();

            connectionInterface.connectionEstablish();
        } catch (IOException e) {
            connectionInterface.failedToConnect(information="Failed to establish server.");
        }
    }

    private void startNewClientListening(){
        executorService.execute(() -> {
            while(isConnected && !serverSocket.isClosed()) {
                try {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    executorService.execute(() -> {
                        handleClientSocket(clientSocket);
                    });
                } catch (IOException e) {
                    connectionInterface.failedToSearchNewClient();
                }
            }
        });
    }
    private void handleClientSocket(SSLSocket clientSocket){
        try {
            connectionsSocket.add(clientSocket);
            connectionsOutputStream.add(new ObjectOutputStream(clientSocket.getOutputStream()));
            numOfUser++;
            connectionsOutputStream.getLast().writeObject(new ConnectionMsg(DataMsgType.UID_GIVER, "" + numOfUser));
            connectionsOutputStream.getLast().flush();
            startNewClientListening();
            String username = "";

            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            while(isConnected) {
                Object obj = ois.readObject();
                if (obj instanceof ConnectionMsg connectionMsg) { // obj instanceof ConnectionMsg & ConnectionMsg connectionMsg;
                    if (connectionMsg.msgType == DataMsgType.JOINED) {
                        username = connectionMsg.username;
                        for (int i = 0; i < connectionsSocket.size(); i++)
                            if (connectionsSocket.get(i) != clientSocket)
                                connectionsOutputStream.get(i).writeObject(new ConnectionMsg(DataMsgType.JOINED, username));
                        connectionInterface.userJoined(connectionMsg);
                    } else if (connectionMsg.msgType == DataMsgType.LEFT) {
                        throw new IOException();
                    }
                } else {
                    for (int i = 0; i < connectionsSocket.size(); i++) {
                        if (connectionsSocket.get(i) != clientSocket) {
                            connectionsOutputStream.get(i).writeObject(obj);
                            connectionsOutputStream.get(i).flush();
                        }
                    }
                    connectionInterface.gotData(obj);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            ConnectionMsg leftMsg = new ConnectionMsg(DataMsgType.LEFT, username);
            try {
                for (int i = connectionsSocket.indexOf(clientSocket) + 1; i < connectionsSocket.size(); i++) {
                    connectionsOutputStream.get(i).writeObject(new ConnectionMsg(DataMsgType.UID_GIVER, "" + (i + 1)));
                    connectionsOutputStream.get(i).flush();
                }
                for (int i = 0; i < connectionsSocket.size(); i++)
                    connectionsOutputStream.get(i).writeObject(leftMsg);
            }catch(IOException ioE){
                connectionInterface.somethingWentWrong(information="Failed to remove client.");
            }finally {
                connectionsOutputStream.remove(connectionsSocket.indexOf(clientSocket));
                connectionsSocket.remove(clientSocket);
                numOfUser--;
                connectionInterface.userLeft(leftMsg);
            }
        }
    }

    public <UserMsg> int sendData(UserMsg userMsg){
        try {
            if(isServer){
                for (ObjectOutputStream oos : connectionsOutputStream) {
                    oos.writeObject(userMsg);
                    oos.flush();
                }
                return 0;
            }else {
                outputStream.writeObject(userMsg);
                outputStream.flush();
                return 0;
            }
        }catch(IOException e){
            return 1;
        }
    }
    public void close(){
        if(isServer){
            try {
                for (int i = 0; i < connectionsSocket.size(); i++) {
                    connectionsOutputStream.get(i).writeObject(new ConnectionMsg(DataMsgType.LEFT, username));
                    connectionsOutputStream.get(i).flush();
                    connectionsOutputStream.get(i).close();
                    connectionsSocket.get(i).close();
                }
                isConnected = false;
                connectionsOutputStream.clear();
                connectionsSocket.clear();
                serverSocket.close();
                serverSocket = null;
                executorService.shutdownNow();
                UID = 0;
            }catch(IOException e){
                connectionInterface.somethingWentWrong(information="Something went wrong while closing the connection.");
            }
        }else{
            try {
                sendData(new ConnectionMsg(DataMsgType.LEFT, username));
                isConnected = false;
                socket.close();
                socket = null;
                outputStream = null;
                UID = 0;
            }catch(IOException e){
                connectionInterface.somethingWentWrong(information="Something went wrong while closing the connection.");
            }
        }
    }

    public String getUsername(){return username;}
    public int getUserID(){return UID;}
    public int getNumberOfUser(){return numOfUser;}
    public boolean isConnected(){return isConnected;}
    public boolean isServer(){return isServer;}
}
