package CodedForOnlyTCP;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO: When server Leaves, make that
// TODO: When server leaves abruptly
// TODO: Send new join Client Chat history
// TODO: Make something for setting
// TODO: Make it possible to join via bluetooth also
// TODO: Automatically search for IP Address and Port number
// TODO: Connect it with Firebase also
// TODO: Make UI also

// _help for command menu
// _setting for settings
/*
* __SETTING__
* Change IP Address
* Change Port Number
*
* */
// _exit to exit chat

enum Page{
    CHAT,
    SETTING,
    HOME
}

public class ChatWM {

    String username;
    int portNum;
    String ipAddress;
    ServerSocket serverSocket;
    Socket socket;
    Scanner inputScanner = new Scanner(System.in);
    ArrayList<UserMessage> chatMsg = new ArrayList<>();

    ExecutorService executorService;
    ArrayList<Socket> connections;
    ArrayList<ObjectOutputStream> connectionsOutputStream;
    int UID, numOfUser;
    boolean isConnected = false;

    // boolean isExit = false;
    Page currentPage = Page.HOME;

    String information = "";

    ChatWM(){
        getInitialUserData();
    }

    void startJoining(){
        // port must be empty
        inputScanner = new Scanner(System.in);
        information = "";
        try {
            socket = new Socket(ipAddress, portNum);
            executorService = Executors.newFixedThreadPool(2);
            isConnected = true;

            try {
                executorService.execute(() -> {
                    try {
                        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                        chatMsg.add(new UserMessage(MsgType.JOINED, "You", ""));
                        outputStream.writeObject(new UserMessage(MsgType.JOINED, username, ""));
                        outputStream.flush();
                        updateFrame();
                        do {
                            String msg = inputScanner.nextLine();
                            if(!isConnected){
                                socket.close();
                                socket = null;
                                startJoining();
                                updateFrame();
                                break;
                            }
                            if("_exit".equalsIgnoreCase(msg.trim())){
                                outputStream.writeObject(new UserMessage(MsgType.LEFT, username, ""));
                                outputStream.flush();
                                currentPage = Page.HOME;
                                information = "You Left the Chat!";
                                isConnected = false;
                                chatMsg.clear();
                                outputStream.close();
                                socket.close();
                                updateFrame();
                                break;
                            }else if("_clear".equals(msg)){
                                chatMsg.clear();
                            }else {
                                chatMsg.add(new UserMessage(MsgType.CHAT, username, msg));
                                outputStream.writeObject(chatMsg.getLast());
                                outputStream.flush();
                            }
                            updateFrame();
                        } while (true);
                    } catch (IOException e) {
                        System.out.println("Yaha ka hai");
                        e.printStackTrace();
                    }
                });

                executorService.execute(() -> {
                    try {
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        do {
                            UserMessage um = (UserMessage) ois.readObject();
                            if(um.msgType==MsgType.UID_GIVER) {
                                UID = Integer.parseInt(um.message);
                            } else if (um.msgType==MsgType.CLIENT_STATUS) {
                                numOfUser = Integer.parseInt(um.message);
                            }else{
                                chatMsg.add(um);
                            }
                            updateFrame();
                        } while (currentPage==Page.CHAT);
                    } catch (IOException e) { // EOFException
                        if(currentPage==Page.CHAT) {
                            isConnected = false;
                            information = "Server left! Enter any msg to reconnect...";
                            updateFrame();
                        }
                    } catch (ClassNotFoundException e) {
                        System.out.println("Received Corrupted Message!");
                    }
                });
                executorService.shutdown();
            } catch (RuntimeException e) {
                System.err.println("Client exception: " + e.getMessage());
            }
        } catch (IOException er) {
            executorService = Executors.newCachedThreadPool();
            try {
                serverSocket = new ServerSocket(portNum); // new ServerSocket(portNum, 50, InetAddress.getByName(ipAddress));
                portNum = serverSocket.getLocalPort();
                ipAddress = InetAddress.getLocalHost().getHostAddress();
                connections = new ArrayList<>();
                connectionsOutputStream = new ArrayList<>();
                chatMsg.add(new UserMessage(MsgType.JOINED, "You", ""));
                isConnected = true;
                UID = 1;
                numOfUser = 1;

                executorService.execute(() -> {
                    try {
                        do {
                            String msg = inputScanner.nextLine();
                            if("_exit".equals(msg)){
                                currentPage = Page.HOME;
                                information = "You Left the Chat!";
                                isConnected = false;
                                chatMsg.clear();
                                for(int i=0; i<connections.size(); i++){
                                    connectionsOutputStream.get(i).writeObject(new UserMessage(MsgType.LEFT, username, ""));
                                    connectionsOutputStream.get(i).flush();
                                    connections.get(i).close();
                                    connectionsOutputStream.get(i).close();
                                }
                                connectionsOutputStream.clear();
                                connections.clear();
                                serverSocket.close();
                                serverSocket = null;
                                executorService.shutdown();
                            }else if("_clear".equals(msg)){
                                chatMsg.clear();
                                updateFrame();
                            }else {
                                chatMsg.add(new UserMessage(MsgType.CHAT, username, msg));
                                for (ObjectOutputStream oos : connectionsOutputStream) {
                                    oos.writeObject(chatMsg.getLast());
                                    oos.flush();
                                }
                                updateFrame();
                            }
                        } while (currentPage==Page.CHAT);
                        updateFrame();
                    } catch (EOFException e) {
                        System.out.println("Server left! 2");
                    } catch (IOException e) {
                        System.out.println("Unable to send msg!");
                    }
                });

                startNewClientListening();

            } catch (IOException e) {
                System.err.println("Server exception: " + e.getMessage());
            }
        }
    }

    void startNewClientListening(){
        executorService.execute(() -> {
            try (Socket clientSocket = serverSocket.accept()) {
                connections.add(clientSocket);
                connectionsOutputStream.add(new ObjectOutputStream(clientSocket.getOutputStream()));
                numOfUser++;
                connectionsOutputStream.getLast().writeObject(new UserMessage(MsgType.UID_GIVER, "", ""+numOfUser));
                connectionsOutputStream.getLast().flush();
                startNewClientListening();
                for (int i=0; i<connections.size(); i++)
                    connectionsOutputStream.get(i).writeObject(new UserMessage(MsgType.CLIENT_STATUS, "", "" + numOfUser));

                ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                UserMessage um;
                do {
                    try {
                        um = (UserMessage) ois.readObject();
                        chatMsg.add(um);
                        for (int i=0; i<connections.size(); i++)
                            if (connections.get(i) != clientSocket) {
                                connectionsOutputStream.get(i).writeObject(um);
                                connectionsOutputStream.get(i).flush();
                            }
                        updateFrame();
                    } catch (IOException | ClassNotFoundException e) {
                        // Abruptly client leaves
                        if(currentPage==Page.CHAT) {
                            for (int i=connections.indexOf(clientSocket)+1; i<connections.size(); i++) {
                                connectionsOutputStream.get(i).writeObject(new UserMessage(MsgType.UID_GIVER, "", "" + (i + 1)));
                                connectionsOutputStream.get(i).flush();
                            }
                            connectionsOutputStream.remove(connections.indexOf(clientSocket));
                            connections.remove(clientSocket);
                            numOfUser--;
                            for (int i=0; i<connections.size(); i++)
                                connectionsOutputStream.get(i).writeObject(new UserMessage(MsgType.CLIENT_STATUS, "", "" + numOfUser));
                            updateFrame();
                            break;
                        }
                    }
                }while (currentPage==Page.CHAT);

                // System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress());
                //BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                //System.out.println("Received from client: " + in.readLine()); // Make this part Only to read from respective Clinets

            }catch (IOException e){
                if(currentPage==Page.CHAT) {
                    System.out.println("Error while searching for new Client! " + e.getMessage());
                }
            }
        });
    }



    synchronized void updateFrame(){
        System.out.print("\033[3J\033[2J\033[H"); // \033[3J -> Clears the scrollback buffer, \033[2J -> Clears the entire screen, \033[H  -> Moves the cursor to top-left corner (0,0)
        System.out.flush();
        String input;
        switch (currentPage){
            case HOME:
                System.out.println("-------------------- Home Page --------------------");
                System.out.println();
                System.out.println("Command Code        Options");
                System.out.println("#####################################");
                System.out.println("    1            Enter Chat Room");
                System.out.println("    2               Settings");
                System.out.println("    3                 Exit");
                System.out.println();
                System.out.println(("".equals(information)) ? "": "Info: "+information);
                System.out.print("\nEnter Command Code: ");
                input = inputScanner.nextLine();
                if ("1".equals(input)){
                    currentPage = Page.CHAT;
                    information = "";
                    startJoining();
                }else if("2".equals(input)){
                    currentPage = Page.SETTING;
                }else if("3".equals(input)){
                    System.exit(0);
                }else{
                    information = "Enter Correct Value!";
                }
                updateFrame();
                break;
            case SETTING:
                System.out.println("-------------------- Settings --------------------");
                System.out.println();
                System.out.println("Command Code               Options");
                System.out.println("#################################################");
                System.out.println("    1            IP Address    :    "+ipAddress);
                System.out.println("    2           Port Number    :    "+portNum);
                System.out.println("    3                      Go Back");
                System.out.println();
                System.out.println(("".equals(information)) ? "": "Info: "+information);
                System.out.print("\nEnter Command Code: ");
                input = inputScanner.nextLine();
                if ("1".equals(input)){
                    System.out.print("Enter IP Address (Leave empty for localhost): ");
                    ipAddress = inputScanner.nextLine();
                    if(Objects.equals(ipAddress, "")) ipAddress = "localhost";
                }else if("2".equals(input)){
                    System.out.print("Enter Port Number to Connect: ");
                    do {
                        try {
                            String inp = inputScanner.nextLine();
                            portNum = ("".equals(inp)) ? 0 :Integer.parseInt(inp);
                            break;
                        } catch (Exception e) {
                            System.out.println("\nPlease Enter Correct Value!");
                            System.out.print("Enter Port Number to Connect: ");
                        }
                    }while(true);
                }else if("3".equals(input)){
                    currentPage = Page.HOME;
                }else{
                    information = "Enter Correct Value!";
                }
                updateFrame();

                break;
            case CHAT:
                System.out.println(username+"  UserID:"+UID+"  "+((isConnected) ? "": "Not ")+"Connected  "+((serverSocket!=null) ? "Server": ((socket!=null) ? "Client": ""))+"  "+numOfUser+" Online");
                System.out.println("-------------------- Welcome To Chat Room --------------------");
                long currentTime = Instant.now().getEpochSecond();
                for (int i=0; i<chatMsg.size(); i++) {
                    long delta = currentTime-chatMsg.get(i).epochSeconds;
                    System.out.println(
                            switch (chatMsg.get(i).msgType) {
                                case MsgType.CHAT -> "  "+chatMsg.get(i).username + "\t:\t" + chatMsg.get(i).message;
                                case MsgType.JOINED -> "-> " + chatMsg.get(i).username + " joined the chat!";
                                case MsgType.LEFT -> "-> " + chatMsg.get(i).username + " has left the chat!";
                                default -> "";
                            }+" ("+((delta<60) ? delta+" seconds": ((delta/=60)<60) ? delta+" minutes": (delta/60)+" hours")+" ago)");
                }
                System.out.println("\n");
                if (!"".equals(information)) System.out.println("Info: "+information);
                System.out.println("Enter your msg to send: ");
                break;
        }
    }

    void getInitialUserData(){
        inputScanner = new Scanner(System.in);
        do{
            System.out.print("Enter Your Name: ");
            username = inputScanner.nextLine();
            if(Objects.equals(username, ""))
                username = "User"+((int) (Math.random() * 10000));
            else if(username.length()>15)
                System.out.println("Username must not be more than 15 characters!");
            else
                break;
        }while(true);

        System.out.print("Enter IP Address (Leave empty for localhost): ");
        ipAddress = inputScanner.nextLine();
        if(Objects.equals(ipAddress, "")) ipAddress = "localhost";

        System.out.print("Enter Port Number to Connect: ");
        do {
            try {
                String inp = inputScanner.nextLine();
                portNum = ("".equals(inp)) ? 0 :Integer.parseInt(inp);
                break;
            } catch (Exception e) {
                System.out.println("\nPlease Enter Correct Value!");
                System.out.print("Enter Port Number to Connect: ");
            }
        }while(true);

        updateFrame();
    }


    static void main() {
        new ChatWM();
    }
}
