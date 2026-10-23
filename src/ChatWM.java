import com.networking.ConnectionMsg;
import com.networking.DataMsgType;
import com.networking.TCP_TLS_Connection;
import com.networking.TCP_TLS_ConnectionInterface;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

enum Page{
    CHAT,
    SETTING,
    HOME,
    CONNECT_BY
}

public class ChatWM implements TCP_TLS_ConnectionInterface {

    TCP_TLS_Connection connection;
    Scanner inputScanner;
    String username;
    Page currentPage = Page.HOME;
    List<Object> chatMsg = new CopyOnWriteArrayList<>();
    boolean isRejoin = false;

    public ChatWM(){
        getInitialUserData();
        updateFrame();
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
        String ipAddress = inputScanner.nextLine();
        if(Objects.equals(ipAddress, "")) ipAddress = "localhost";

        System.out.print("Enter Port Number to Connect: ");
        int portNum;
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

        connection = new TCP_TLS_Connection(username, ipAddress, portNum, this);
    }

    @Override
    public void connectionEstablished(){
        inputScanner = new Scanner(System.in);
        ChatMsg chat;
        String input;
        updateFrame();
        input = inputScanner.nextLine();
        while(connection.isConnected()){
            if(checkCommands(input)) {
                chat = new ChatMsg(username, input);
                connection.sendData(chat);
                chatMsg.add(chat);
                updateFrame();
            }
            input = inputScanner.nextLine();
        }
        if (isRejoin){
            isRejoin = false;
            connection.startConnection();
        }
    }
    boolean checkCommands(String input){
        switch (input){
            case "_exit":
                connection.close();
                break;
            case "_clear":
                chatMsg.clear();
                break;
            default:
                return true;
        }
        return false;
    }

    @Override
    public <UserMsg> void gotData(UserMsg userMsg) {
        chatMsg.add(userMsg);
        updateFrame();
    }

    @Override
    public void userJoined(ConnectionMsg connectionMsg){
        chatMsg.add(connectionMsg);
        updateFrame();
    }

    @Override
    public void userLeft(ConnectionMsg connectionMsg){
        chatMsg.add(connectionMsg);
        updateFrame();
    }

    @Override
    public void failedToConnect(String information){
        System.out.println("Info: "+information);
        System.out.println("Enter anything to re-establish the connection");
        isRejoin = true;
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
                System.out.println(("".equals(connection.information)) ? "": "Info: "+connection.information);
                System.out.print("\nEnter Command Code: ");
                input = inputScanner.nextLine();
                if ("1".equals(input)){
                    currentPage = Page.CONNECT_BY;
                    connection.information = "";
                }else if("2".equals(input)){
                    currentPage = Page.SETTING;
                    connection.information = "";
                }else if("3".equals(input)){
                    System.exit(0);
                }else{
                    connection.information = "Enter Correct Value!";
                }
                updateFrame();
                break;
            case CONNECT_BY:
                System.out.println("-------------------- Connect By --------------------");
                System.out.println();
                System.out.println("Command Code          Options");
                System.out.println("#####################################");
                System.out.println("    1            TCP TLS Connection");
                System.out.println("    2                 Go Back");
                System.out.println();
                System.out.println(("".equals(connection.information)) ? "": "Info: "+connection.information);
                System.out.print("\nEnter Command Code: ");
                input = inputScanner.nextLine();
                if ("1".equals(input)){
                    currentPage = Page.CHAT;
                    connection.information = "";
                    connection.startConnection();
                }else if("2".equals(input)){
                    currentPage = Page.HOME;
                    connection.information = "";
                }else{
                    connection.information = "Enter Correct Value!";
                }
                updateFrame();
                break;
            case SETTING:
                System.out.println("-------------------- Settings --------------------");
                System.out.println();
                System.out.println("Command Code               Options");
                System.out.println("#################################################");
                System.out.println("    1            IP Address    :    "+connection.ipAddress);
                System.out.println("    2           Port Number    :    "+connection.portNum);
                System.out.println("    3                      Go Back");
                System.out.println();
                System.out.println(("".equals(connection.information)) ? "": "Info: "+connection.information);
                System.out.print("\nEnter Command Code: ");
                input = inputScanner.nextLine();
                if ("1".equals(input)){
                    System.out.print("Enter IP Address (Leave empty for localhost): ");
                    connection.ipAddress = inputScanner.nextLine();
                    if(Objects.equals(connection.ipAddress, "")) connection.ipAddress = "localhost";
                }else if("2".equals(input)){
                    System.out.print("Enter Port Number to Connect: ");
                    do {
                        try {
                            String inp = inputScanner.nextLine();
                            connection.portNum = ("".equals(inp)) ? 0 :Integer.parseInt(inp);
                            break;
                        } catch (Exception e) {
                            System.out.println("\nPlease Enter Correct Value!");
                            System.out.print("Enter Port Number to Connect: ");
                        }
                    }while(true);
                }else if("3".equals(input)){
                    currentPage = Page.HOME;
                    connection.information = "";
                }else{
                    connection.information = "Enter Correct Value!";
                }
                updateFrame();
                break;
            case CHAT:
                System.out.println(connection.getUsername()+"  UserID:"+connection.getUserID()+"  "+((connection.isConnected()) ? "": "Not ")+"Connected  "+((connection.isServer()) ? "Server": "Client")+"  "+connection.getNumberOfUsers()+" Online");
                System.out.println("-------------------- Welcome To Chat Room --------------------");
                long currentTime = Instant.now().getEpochSecond();
                for (Object o : chatMsg) {
                    if (o instanceof ChatMsg chat) {
                        long delta = currentTime - chat.epochSeconds;
                        System.out.println("  " + chat.username + "\t:\t" + chat.message
                                + " (" + ((delta < 60) ? delta + " seconds" : ((delta /= 60) < 60) ? delta + " minutes" : (delta / 60) + " hours") + " ago)");
                    } else if (o instanceof ConnectionMsg cont) {
                        long delta = currentTime - cont.getEpochSeconds();
                        System.out.println(
                                switch (cont.msgType) {
                                    case DataMsgType.JOINED -> "-> " + cont.getUsername() + " joined the chat!";
                                    case DataMsgType.LEFT -> "-> " + cont.getUsername() + " has left the chat!";
                                    default -> "";
                                } + " (" + ((delta < 60) ? delta + " seconds" : ((delta /= 60) < 60) ? delta + " minutes" : (delta / 60) + " hours") + " ago)");
                    }
                }
                System.out.println("\n");
                if (!"".equals(connection.information)) System.out.println("Info: "+connection.information);
                System.out.println("Enter your msg to send: ");
                connection.information = "";
                break;
        }
    }

    static void main() {
        new ChatWM();
    }
}
