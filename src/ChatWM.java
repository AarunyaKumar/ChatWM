import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TODO: When server Leaves, make that
// TODO: Send new join Client Chat history
// TODO: Make something for setting
// TODO: Make it possible to join via bluetooth also
// TODO: Automatically search for IP Address and Port number
// TODO: Connect it with Firebase also
// TODO: Make UI also

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

    ChatWM(){
        getInitialUserData();

        // port must be empty
        try {
            socket = new Socket(ipAddress, portNum);
            executorService = Executors.newFixedThreadPool(2);
            try {
                executorService.execute(() -> {
                    try {
                        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                        do {
                            chatMsg.add(new UserMessage(username, inputScanner.nextLine()));
                            outputStream.writeObject(chatMsg.getLast());
                            updateFrame();
                        } while (true);
                    } catch (EOFException e) {
                        System.out.println("Server left! 2");
                    } catch (IOException e) {
                        System.out.println("Unable to send msg!");
                    }
                });

                executorService.execute(() -> {
                    try {
                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                        do {
                            chatMsg.add((UserMessage) ois.readObject());
                            updateFrame();
                        } while (true);
                    } catch (EOFException e) {
                        System.out.println("Server left!");
                    } catch (IOException e) {
                        System.out.println("Some IO Exception Occurred!");
                    } catch (ClassNotFoundException e) {
                        System.out.println("Received Corrupted Message!");
                    }
                });
                executorService.shutdown();
                // close this socket
            } catch (RuntimeException e) {
                System.err.println("Client exception: " + e.getMessage());
            }
        } catch (IOException er) {
            executorService = Executors.newFixedThreadPool(4);
            try {
                serverSocket = new ServerSocket(portNum);
                connections = new ArrayList<>();
                connectionsOutputStream = new ArrayList<>();

                executorService.execute(() -> {
                    try {
                        do {
                            chatMsg.add(new UserMessage(username, inputScanner.nextLine()));
                            for (ObjectOutputStream oos: connectionsOutputStream)
                                oos.writeObject(chatMsg.getLast());
                            updateFrame();
                        } while (true);
                    } catch (EOFException e) {
                        System.out.println("Server left! 2");
                    } catch (IOException e) {
                        System.out.println("Unable to send msg!");
                    }
                });

                startNewClientListening();

                // Close this serverSocket
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
                updateFrame();
                startNewClientListening();

                ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
                UserMessage um;
                do {
                    try {
                        um = (UserMessage) ois.readObject();
                        chatMsg.add(um);
                        for (int i=0; i<connections.size(); i++) {
                            if (connections.get(i) != clientSocket) {
                                connectionsOutputStream.get(i).writeObject(um);
                            }
                        }
                    } /*catch (EOFException e){
                        // Client stream closed
                    }*/catch (IOException | ClassNotFoundException e) {
                        // Abruptly client leaves
                        connectionsOutputStream.remove(connections.indexOf(clientSocket));
                        connections.remove(clientSocket);
                        updateFrame();
                        break;
                    }
                    updateFrame();
                }while (true);

                // System.out.println("Client connected from: " + clientSocket.getRemoteSocketAddress());

                //BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                //System.out.println("Received from client: " + in.readLine()); // Make this part Only to read from respective Clinets

            }catch (IOException e){
                System.out.println("Error while searching for new Client! "+e.getMessage());
            }
        });
    }


    void updateFrame(){
        System.out.print("\033[3J\033[2J\033[H"); // \033[3J -> Clears the scrollback buffer, \033[2J -> Clears the entire screen, \033[H  -> Moves the cursor to top-left corner (0,0)
        System.out.flush();
        System.out.println("--------------------WELCOME TO ChatWM--------------------");
        if(serverSocket!=null)
            System.out.println("Number of Clients: "+connections.size());
        for (UserMessage um: chatMsg){
            System.out.println(um.username+"\t:\t"+um.message);
        }
        System.out.println("\n\n\n");
        System.out.println("Enter your msg to send: ");
    }

    void getInitialUserData(){
        inputScanner = new Scanner(System.in);
        System.out.print("Enter Your Name: ");
        username = inputScanner.nextLine();

        System.out.print("Enter IP Address (Leave empty for localhost): ");
        ipAddress = inputScanner.nextLine();
        if(Objects.equals(ipAddress, "")) ipAddress = "localhost";

        System.out.print("Enter Port Number to Connect: ");
        do {
            try {
                portNum = inputScanner.nextInt();
                break;
            } catch (Exception e) {
                System.out.println("\nPlease Enter Correct Value!");
                System.out.print("Enter Port Number to Connect: ");
            }
        }while(true);

        updateFrame();
        inputScanner = new Scanner(System.in);
    }


    static void main() {
        new ChatWM();
    }
}
