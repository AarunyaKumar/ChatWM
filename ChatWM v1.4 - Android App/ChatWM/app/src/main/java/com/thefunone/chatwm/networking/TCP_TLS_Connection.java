package com.thefunone.chatwm.networking;

import javax.net.ssl.*;
import java.io.*;
import java.net.ConnectException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Functions to call
 *
 * void startConnection();
 * <UserMsg> int sendData(UserMsg userMsg);
 * void close();
 *
 * String getUsername();
 * int getUserID();
 * int getNumberOfUsers();
 * List<String> getAllUsernames();
 * boolean isConnected();
 * boolean isServer();
 * */


public class TCP_TLS_Connection {
    TCP_TLS_ConnectionInterface connectionInterface;

    boolean isConnected = false, isServer = false;
    String username;
    int UID, numOfUser;

    private SSLSocket socket;
    private ObjectOutputStream outputStream;
    private ExecutorService executorService;

    private SSLServerSocket serverSocket;
    private List<SSLSocket> connectionsSocket;
    private List<ObjectOutputStream> connectionsOutputStream;

    public String ipAddress;
    public int portNum;
    public String information = "";
    public List<ConnectionMsg> connectionsMsg;
    public List<String> connectedUsernames;

    private final String KEYSTORE_PATH;
    private final char[] KEYSTORE_PASSWORD;

    String otherFileNames = "";

    public TCP_TLS_Connection(String username, String ipAddress, int portNum, TCP_TLS_ConnectionInterface connectionInterface, String KEYSTORE_PATH, char[] KEYSTORE_PASSWORD, String[] fileNames){
        this.KEYSTORE_PATH = KEYSTORE_PATH;
        this.KEYSTORE_PASSWORD = KEYSTORE_PASSWORD;
        this.username = username;
        this.ipAddress = ipAddress;
        this.portNum = portNum;
        this.connectionInterface = connectionInterface;
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<fileNames.length; i++)
            sb.append(fileNames[i]).append(";");
        otherFileNames = sb.toString();
    }
    private SSLContext createSSLContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream inputStream = new FileInputStream(KEYSTORE_PATH)) {
            keyStore.load(inputStream, KEYSTORE_PASSWORD);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    public final void startConnection(SSLContext sslContext){
        connectionsMsg = new CopyOnWriteArrayList<>();
        connectedUsernames = new CopyOnWriteArrayList<>();
        executorService = Executors.newCachedThreadPool();
        try{
            socket = (SSLSocket) ((sslContext!=null) ? sslContext :createSSLContext()).getSocketFactory().createSocket(ipAddress, portNum);
            socket.setEnabledProtocols(new String[]{"TLSv1.3"});
            socket.setEnabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"});
            socket.startHandshake();
            information = "";
            try{
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                executorService.execute(() -> {
                    try {
                        ObjectInputStream ois = new FilteredObjectInputStream(socket.getInputStream(), otherFileNames + "java.base/*;!*");
                        do {
                            Object obj = ois.readObject();
                            if(obj instanceof ConnectionMsg){
                                ConnectionMsg connectionMsg = (ConnectionMsg) obj;
                                switch (connectionMsg.msgType){
                                    case JOINED:
                                        if(!connectionMsg.username.isEmpty()) {
                                            connectionsMsg.add(connectionMsg);
                                            connectedUsernames.add(connectionMsg.username);
                                            numOfUser++;
                                            executorService.execute(() -> connectionInterface.userJoined(connectionMsg));
                                        }
                                        break;
                                    case LEFT:
                                        if(!connectionMsg.username.isEmpty()) {
                                            connectionsMsg.add(connectionMsg);
                                            connectedUsernames.remove(connectionMsg.username);
                                            numOfUser--;
                                            executorService.execute(() -> connectionInterface.userLeft(connectionMsg));
                                        }
                                        break;
                                    case ASSIGN_UID :
                                        UID = Integer.parseInt(connectionMsg.username);
                                        break;
                                    case CLIENT_JOINED:
                                        sendData(new ConnectionMsg(DataMsgType.CLIENT_JOINED, username));
                                        executorService.execute(() -> {
                                            ConnectionMsg msg = new ConnectionMsg(DataMsgType.JOINED, username);
                                            connectionsMsg.add(msg);
                                            executorService.execute(() -> connectionInterface.connectionEstablished());
                                            connectionInterface.userJoined(msg);
                                        });
                                        break;
                                }
                            }else{
                                executorService.execute(() -> connectionInterface.gotData(obj));
                            }
                        } while (isConnected);
                    } catch (IOException | ClassNotFoundException e) { // EOFException
                        if(isConnected) {
                            isConnected = false;
                            executorService.execute(() -> connectionInterface.failedToConnect(information=(e instanceof IOException) ? "Server left! Try to reconnect.": "Received Corrupted Message"));
                            executorService.shutdown();
                        }
                    }
                });
                isConnected = true;
                isServer = false;
                numOfUser = 1;
                connectedUsernames.add(username);
            }catch (RuntimeException e){
                connectionInterface.somethingWentWrong(information="Something went wrong in threading. Please restart your application");
            }
        }catch(SSLException e){
            connectionInterface.somethingWentWrong(information="It's not our server!");
        }catch (ConnectException e){
            startConnectionAsServer(sslContext);
        } catch(IOException e){
            executorService.execute(() -> connectionInterface.failedToConnect(information="Please retry! "+e.getMessage())); // Some general exception ask user to re-establish connection
        }catch(Exception e){
            executorService.execute(() -> connectionInterface.failedToConnect(information="Please retry! "+e.getMessage()));
        }
    }
    private void startConnectionAsServer(SSLContext sslContext) {
        try {
            serverSocket = (SSLServerSocket) ((sslContext!=null) ? sslContext: createSSLContext()).getServerSocketFactory().createServerSocket(portNum);
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.3"});
            serverSocket.setEnabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"});
            serverSocket.setNeedClientAuth(true);
        } catch (Exception e) {
            executorService.execute(() -> connectionInterface.failedToConnect(information="Please retry! "+e.getMessage()));
            return;
        }
        connectionsSocket = new CopyOnWriteArrayList<>();
        connectionsOutputStream = new CopyOnWriteArrayList<>();

        isConnected = true;
        isServer = true;
        UID = 1;
        numOfUser = 1;
        connectedUsernames.add(username);
        information = "";

        startNewClientListening();

        executorService.execute(() -> {
            ConnectionMsg msg = new ConnectionMsg(DataMsgType.JOINED, username);
            connectionsMsg.add(msg);
            executorService.execute(() -> connectionInterface.connectionEstablished());
            connectionInterface.userJoined(msg);
        });
    }

    private void startNewClientListening(){
        executorService.execute(() -> {
            while(isConnected && !serverSocket.isClosed()) {
                try {
                    SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                    executorService.execute(() -> {
                        try {
                            clientSocket.setSoTimeout(3000);
                            clientSocket.startHandshake(); // checks mutual certificates
                            clientSocket.setSoTimeout(0);
                            handleClientSocket(clientSocket);
                        }catch(Exception e){
                            try {
                                clientSocket.close();
                            }catch (IOException ignored){}
                        }
                    });


                } catch (IOException e) {
                    connectionInterface.failedToSearchNewClient();
                }
            }
        });
    }
    private void handleClientSocket(SSLSocket clientSocket){
        String username = "";
        boolean isUserAdded = false;
        AtomicBoolean isUserJoinedCalled = new AtomicBoolean(false);
        try {
            synchronized (this) {
                connectionsOutputStream.add(new ObjectOutputStream(clientSocket.getOutputStream()));
                connectionsSocket.add(clientSocket);
                numOfUser++;
                isUserAdded = true;
                connectionsOutputStream.get(connectionsOutputStream.size()-1).writeObject(new ConnectionMsg(DataMsgType.ASSIGN_UID , "" + numOfUser));
                for (ConnectionMsg msg : connectionsMsg)
                    connectionsOutputStream.get(connectionsOutputStream.size()-1).writeObject(msg);
                connectionsOutputStream.get(connectionsOutputStream.size()-1).writeObject(new ConnectionMsg(DataMsgType.CLIENT_JOINED, ""));
                connectionsOutputStream.get(connectionsOutputStream.size()-1).flush();
            }

            ObjectInputStream ois = new FilteredObjectInputStream(clientSocket.getInputStream(), otherFileNames + "java.base/*;!*");
            while(isConnected) {
                Object obj = ois.readObject();
                if (obj instanceof ConnectionMsg) { // obj instanceof ConnectionMsg & ConnectionMsg connectionMsg;
                    ConnectionMsg connectionMsg = (ConnectionMsg) obj;
                    if (connectionMsg.msgType == DataMsgType.JOINED || connectionMsg.msgType == DataMsgType.CLIENT_JOINED) {
                        if(connectionMsg.msgType == DataMsgType.CLIENT_JOINED) {
                            username = connectionMsg.username;
                            connectedUsernames.add(username);
                        }
                        ConnectionMsg msg = new ConnectionMsg(DataMsgType.JOINED, connectionMsg.username);
                        connectionsMsg.add(msg);
                        synchronized (this) {
                            for (int i = 0; i < connectionsSocket.size(); i++)
                                if (connectionsSocket.get(i) != clientSocket)
                                    connectionsOutputStream.get(i).writeObject(msg);
                        }
                        executorService.execute(() -> {
                            isUserJoinedCalled.set(true);
                            connectionInterface.userJoined(msg);
                        });
                    } else if (connectionMsg.msgType == DataMsgType.LEFT) {
                        throw new IOException();
                    }
                } else {
                    synchronized (this) {
                        for (int i = 0; i < connectionsSocket.size(); i++) {
                            if (connectionsSocket.get(i) != clientSocket) {
                                connectionsOutputStream.get(i).writeObject(obj);
                                connectionsOutputStream.get(i).flush();
                            }
                        }
                    }
                    executorService.execute(() -> {
                        connectionInterface.gotData(obj);
                    });
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            ConnectionMsg leftMsg = new ConnectionMsg(DataMsgType.LEFT, username);
            connectionsMsg.add(leftMsg);
            if(isUserAdded)
                numOfUser--;
            if (isUserJoinedCalled.get() && !username.isEmpty())
                executorService.execute(() -> connectionInterface.userLeft(leftMsg));
            try {
                synchronized (this) {
                    for (int i = connectionsSocket.indexOf(clientSocket) + 1; i < connectionsSocket.size(); i++) {
                        connectionsOutputStream.get(i).writeObject(new ConnectionMsg(DataMsgType.ASSIGN_UID, "" + (i + 1)));
                        connectionsOutputStream.get(i).flush();
                    }
                    for (int i = 0; i < connectionsSocket.size(); i++) {
                        if (connectionsSocket.get(i) != clientSocket)
                            try {
                                connectionsOutputStream.get(i).writeObject(leftMsg);
                            } catch (Exception ignored) {
                            }
                    }
                }
            }catch(IOException ioE){
                connectionInterface.somethingWentWrong(information="Client Gone Abruptly");
            }finally {
                if(connectionsSocket.contains(clientSocket)) {
                    synchronized (this) {
                        connectionsOutputStream.remove(connectionsSocket.indexOf(clientSocket));
                        connectionsSocket.remove(clientSocket);
                        connectedUsernames.remove(username);
                    }
                }
            }
        }
    }

    public <UserMsg> int sendData(UserMsg userMsg){
        try {
            if(isServer){
                for (ObjectOutputStream oos : connectionsOutputStream) {
                    try {
                        synchronized (this) {
                            oos.writeObject(userMsg);
                            oos.flush();
                        }
                    }catch(IOException ignored){}
                }
                return 0;
            }else {
                if(outputStream!=null) {
                    outputStream.writeObject(userMsg);
                    outputStream.flush();
                }
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
                    try{
                        connectionsOutputStream.get(i).writeObject(new ConnectionMsg(DataMsgType.LEFT, username));
                        connectionsOutputStream.get(i).flush();
                    }catch(IOException ignored){}
                    try {
                        connectionsOutputStream.get(i).close();
                        connectionsSocket.get(i).close();
                    }catch(IOException ignored){}
                }
                isConnected = false;
                connectionsOutputStream.clear();
                connectionsSocket.clear();
                executorService.shutdownNow();
                serverSocket.close();
                serverSocket = null;
                UID = 0;
            }catch(IOException e){
                connectionInterface.somethingWentWrong(information="Something went wrong while closing the connection.");
            }
        }else{
            try {
                sendData(new ConnectionMsg(DataMsgType.LEFT, username));
                isConnected = false;
                if(socket!=null) {
                    socket.close();
                    socket = null;
                }
                outputStream = null;
                UID = 0;
                if(executorService!=null) {
                    executorService.shutdown();
                }
            }catch(IOException e){
                connectionInterface.somethingWentWrong(information="Something went wrong while closing the connection.");
            }
        }
    }

    public String getUsername(){return username;}
    public int getUserID(){return UID;}
    public int getNumberOfUsers(){return numOfUser;}
    public List<String> getAllUsernames(){return connectedUsernames;}
    public boolean isConnected(){return isConnected;}
    public boolean isServer(){return isServer;}
}
