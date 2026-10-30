```
Create A Java File named SetUpWindow

This will create a JFrame which will have

JLabel containing "Your Name" text with a JTextField infront of it

JLabel containing "Enter IP Address" text with a JTextField infront of it

JLabel containing "Enter Port Number" text with a Field infront of it taking natural numbers only

JLabel containing "Choose Key Store File" text with a area containing default selected File System.getProperty("user.home")+"\\ChatWM\\defaultKeyStore.p12" in from of it and an option to select another file.

JLabel containing "KeyStore Password" text with a password kind of JTextField

Two JButtons "Cancel" and "Connect"

  

  

Working

When user clicks Cancel, just close the window

When user clicks Connect, add a CircularProgressIndicator and add a text "Connecting... " below it, and make a method closeSetUp() so that when I call it, this window and loading closes.

ChatWM chatWM;

SetUpWindow(ChatWM chatWM){

this.chatWM = chatWM;

}

Also, when Connecting... text appears call the function chatWM.connect(String username, String ipAddress, int portNum, String keystorePath, String password);

  

Also create a binary file named "data" at the location System.getProperty("user.home")+"\\ChatWM" containing data of String username, String ipAddress, int portNum, String keystorePath. And fill the text area if binary file already exists and contains the data. When I call closeSetUp() function, save all the changes made in the binary file.

  

Ask/Tell me if anything else is necessary or there's any confusion
```


----------------------------------------------------------

```
In Port Number take values from 0 to 65535, if a user leaves it then send 0 as portNum, if ipAddress if blank send localhost as String also add it to placeholder. When click Browse, it should start with appDir directory. For defaultKeyStore.p12 enter "password123" bydefault otherwise for others .

Make the resetLoading(String errorMessage) function.

Also, add a button to create a new key Store file, take inputs and create at appDir directory
```


-----------------------------------------------------

```

Don't let user pick file other than .p12 and .pfx

Also add a Logo.png

Don't connect if the password field is empty

Add an eye option beside password field so as to view and hide the password

Make the UI bit like Windows 11

```

--------------------------------------------------

```

Create a JFrame for Chating area in Windows 11 style. file name is ChatWindow.java, add

  

Menu Bar as

  

Control

  

|->Go to Set-Up Window // closeChatWindow(); chatWM.setSetUpWindow();

  

Exit

  

|->Exit from Connection // closeChatWindow();

Add a top bar where

  

ipAddress+":"+portNum // In large font

  

username // Write this in small text just below ipAddress

  

// Add the below three from the right side

  

"UID:"+chatManager.UserID

  

(chatManager.connection.isServer()) ? "Server": "Client"

  

(green dot) and then chatManager.connection.getNumberOfUsers()+" Online" // When user hovers it, display a list of usernames below the cursor, call the function chatManager.connection.getAllUsernames() to get List

Add a Panel for chat area

  

add the messages when addMessage function is called

Add a Chat typing area, Add a large text area, add an option to make it alert message type or not, add an option of attach file, when click ask user to pick a file, then check whether the file is less than 200 MB or not to actually send it, add a button to send message.

  

call the chatManager.connection.sendData(ChatMsg);addMessage(...);

ChatMsg.java

  

`import java.io.Serializable;

  

import java.time.Instant;

enum ChatMsgType{

  

SIMPLE_CHAT,

  

ALERT,

  

FILE

  

}

public class ChatMsg implements Serializable {

  

ChatMsgType chatMsgType;

  

String username, message;

  

long epochSeconds;

  

byte[] fileData;

ChatMsg(ChatMsgType chatMsgType, String username, String message) {

this.chatMsgType = chatMsgType;

this.username = username;

this.message = message;

epochSeconds = Instant.now().getEpochSecond();

}

ChatMsg(ChatMsgType chatMsgType, String username, String message, byte[] fileData) {

this(chatMsgType, username, message);

this.fileData = fileData;

}

long getEpochSeconds(){

return epochSeconds;

}

String getUsername(){

return username;

}

}`

Create functions

  

createFrame(); // Don't write frame stuff in constructor

  

closeChatWindow();

  

addMessage(DisplayMessageType dmt, String username, String message, byte[] fileData);

  

enum DisplayMessageType{

  

JOINED, // if this is then add text, "-> "+username+" joined the chat!" (username in dark green color rest in little green color)

  

LEFT, // if this is then add text, "-> "+username+" left the chat!" (username in dark red color rest little red color)

  

SIMPLE_CHAT, // if this is then add text, username and then a line then message

  

ALERT_CHAT, // if this is then add text, username and then a line then message in red color

  

FILE // if this is then add text, username and then a line then message and add a below button download file. Implement the functioning of the button asking user where to download and then save that file

  

}// Maintain UI

ChatWindow.java

  

`

enum DisplayMessageType{

  

JOINED,

  

LEFT,

  

SIMPLE_CHAT,

  

ALERT_CHAT,

  

FILE

  

}

public class ChatWindow {

ChatManager chatManager;

ChatWM chatWM;

  

ChatWindow(ChatWM chatWM, String username, String ipAddress, int portNum, String keystorePath, String password){

this.chatWM = chatWM;

chatManager = new ChatManager(this, username, ipAddress, portNum, keystorePath, password);

}

  

  

void createFrame(){

  

}

void closeChatWindow(){

  

}

  

void addMessage(DisplayMessageType dmt, String username, String message, byte[] fileData){

  

}

}
```


  

---------------------------------------------------------


```
In byte[] set first to length of file name including file extenstion followed by filename and then file data and send and recieve this byte[]

  

Enter to send message and Shift+Enter for new line in text

  

Add a function showInfoLog(String information) to show a Label on top, and it removes after 10seconds, also add a cross button on top right of this Label

Suggest more Menu Options

  

Give me only changed code
```

------------------------

```
void updateStatus(int UID, boolean isServer, int numberOfOnline){

  

}

make this function
```



