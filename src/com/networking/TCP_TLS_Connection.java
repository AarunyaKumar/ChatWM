package com.networking;

import javax.net.ssl.*;
import java.io.*;
import java.net.ConnectException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private final String KEYSTORE_PATH = "chat_keystore.p12";
    private final char[] KEYSTORE_PASSWORD = "write your password here".toCharArray();

    public TCP_TLS_Connection(String username, String ipAddress, int portNum, TCP_TLS_ConnectionInterface connectionInterface){
        this.username = username;
        this.ipAddress = ipAddress;
        this.portNum = portNum;
        this.connectionInterface = connectionInterface;
    }
    private SSLContext createSSLContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream is = new FileInputStream(KEYSTORE_PATH)) {
            keyStore.load(is, KEYSTORE_PASSWORD);
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, KEYSTORE_PASSWORD);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

    public final void startConnection(){
        connectionsMsg = new CopyOnWriteArrayList<>();
        executorService = Executors.newCachedThreadPool();
        try{
            socket = (SSLSocket) createSSLContext().getSocketFactory().createSocket(ipAddress, portNum);
            socket.setEnabledProtocols(new String[]{"TLSv1.3"});
            socket.setEnabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"});
            socket.startHandshake();
            information = "";
            try{
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                executorService.execute(() -> {
                    try {
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        do {
                            Object obj = ois.readObject();
                            if(obj instanceof ConnectionMsg connectionMsg){
                                switch (connectionMsg.msgType){
                                    case JOINED:
                                        connectionsMsg.add(connectionMsg);
                                        numOfUser++;
                                        executorService.execute(() -> connectionInterface.userJoined(connectionMsg));
                                        break;
                                    case LEFT:
                                        connectionsMsg.add(connectionMsg);
                                        numOfUser--;
                                        executorService.execute(() -> connectionInterface.userLeft(connectionMsg));
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
            }catch (RuntimeException e){
                connectionInterface.somethingWentWrong(information="Something went wrong in threading. Please restart your application");
            }
        }catch(SSLException e){
            connectionInterface.somethingWentWrong(information="It's not our server!");
        }catch (ConnectException e){
            startConnectionAsServer();
        } catch(IOException e){
            executorService.execute(() -> connectionInterface.failedToConnect(information="Something went wrong. Please retry to join.")); // Some general exception ask user to re-establish connection
        }catch(Exception e){
            executorService.execute(() -> connectionInterface.failedToConnect(information="Something went wrong. Please retry to join."));
        }
    }
    private void startConnectionAsServer() {
        try {
            serverSocket = (SSLServerSocket) createSSLContext().getServerSocketFactory().createServerSocket(portNum);
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.3"});
            serverSocket.setEnabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"});
            serverSocket.setNeedClientAuth(true);
        } catch (Exception e) {
            executorService.execute(() -> connectionInterface.failedToConnect(information="Something went wrong. Please retry to join."));
            return;
        }
        connectionsSocket = new CopyOnWriteArrayList<>();
        connectionsOutputStream = new CopyOnWriteArrayList<>();

        isConnected = true;
        isServer = true;
        UID = 1;
        numOfUser = 1;
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
                            }catch (IOException _){}
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
        try {
            synchronized (this) {
                connectionsOutputStream.add(new ObjectOutputStream(clientSocket.getOutputStream()));
                connectionsSocket.add(clientSocket);
                numOfUser++;
                connectionsOutputStream.getLast().writeObject(new ConnectionMsg(DataMsgType.ASSIGN_UID , "" + numOfUser));
                for (ConnectionMsg msg : connectionsMsg)
                    connectionsOutputStream.getLast().writeObject(msg);
                connectionsOutputStream.getLast().writeObject(new ConnectionMsg(DataMsgType.CLIENT_JOINED, ""));
                connectionsOutputStream.getLast().flush();
            }

            ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
            while(isConnected) {
                Object obj = ois.readObject();
                if (obj instanceof ConnectionMsg connectionMsg) { // obj instanceof ConnectionMsg & ConnectionMsg connectionMsg;
                    if (connectionMsg.msgType == DataMsgType.JOINED || connectionMsg.msgType == DataMsgType.CLIENT_JOINED) {
                        if(connectionMsg.msgType == DataMsgType.CLIENT_JOINED) username = connectionMsg.username;
                        ConnectionMsg msg = new ConnectionMsg(DataMsgType.JOINED, connectionMsg.username);
                        connectionsMsg.add(msg);
                        synchronized (this) {
                            for (int i = 0; i < connectionsSocket.size(); i++)
                                if (connectionsSocket.get(i) != clientSocket)
                                    connectionsOutputStream.get(i).writeObject(msg);
                        }
                        executorService.execute(() -> {
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
                            } catch (Exception _) {
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
                        numOfUser--;
                    }
                    executorService.execute(() -> {
                        connectionInterface.userLeft(leftMsg);
                    });
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
                    }catch(IOException _){}
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
                    try{
                        connectionsOutputStream.get(i).writeObject(new ConnectionMsg(DataMsgType.LEFT, username));
                        connectionsOutputStream.get(i).flush();
                    }catch(IOException _){}
                    try {
                        connectionsOutputStream.get(i).close();
                        connectionsSocket.get(i).close();
                    }catch(IOException _){}
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
                socket.close();
                socket = null;
                outputStream = null;
                UID = 0;
                executorService.shutdown();
            }catch(IOException e){
                connectionInterface.somethingWentWrong(information="Something went wrong while closing the connection.");
            }
        }
    }

    public String getUsername(){return username;}
    public int getUserID(){return UID;}
    public int getNumberOfUsers(){return numOfUser;}
    public boolean isConnected(){return isConnected;}
    public boolean isServer(){return isServer;}
}
