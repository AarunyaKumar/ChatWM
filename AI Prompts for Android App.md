```
Write a XML file and java file for app ChatWM setup activity named activity_main.xml and activity_main.java

At top there should be ChatWM app name mention, add the following items

"Your Name" <TextArea>

"Enter IP Address" <TextArea>

"Enter Port Number" <TextArea> // Takes only numbers 0 to 65535

"Choose Key Store File" <TextArea> <Button "Choose..."> <Button "Create"> // take defaultKeyStore.p12 selected by default (if 'data' file does not exists); for Choose button let user pick only .p12 and .pfx files; for Create button allow user to create and save a keystore field PKCS12 with extension .p12 at desired location.

"KeyStore Password" <TextArea for password> <eye to hide and show password>

<Button on right side "Connect"> // when user clicks, check for valid inputs and if so, enter to another activity named ChatActivity.java and transfer data username, ipAddress, portNum, keystorePath, keystorePassword

  

Also create a private file 'data' and create a function saveData() to save username, ipAddress, portNum, keystorePath.

If this 'data' file exist then check fill the entry by default.

If that defaultKeyStore.p12 file is selected, enter 'password123' in password field.

If ipAddress field is empty then show placeholder 'localhost' and transfer 'localhost' when user clicks Connect button if the ipAddress field is empty.

  

package com.thefunone.chatwm
```


---------------

```
Top ChatWM text is not showing on device.

Update the MainActivity.java and activity_main.xml

Add a variable ChatManager chatManager;

When user clicks Connect add a loading circle on bottom, and initialize and chatManager().

create a function in MainActivity, ChatActivity connectionEstablished() which will remove the loading element and enters into ChatActivity and create a function failedToConnect(String information) which will stop reloading the interface.

  

DisplayMessageType is present on top of class in ChatActivity

enum DisplayMessageType {

JOINED,

LEFT,

SIMPLE_CHAT,

ALERT_CHAT,

FILE

}

  

Now create file for activity_chat.xml and ChatActivity.java where on top bar write these as shown in image

ipAddress+":"+portNum+(some space)+"Host: "+hostname

small username on bottom

show status UID, Server or Client, number of online (when user hovers it show all connected usernames call chatManager.connection.getAllUsernames() to get List<String>)

Create a 3dot on top right which contain one option "Exit Chat", when pressed call chatManager.connection.close();chatManager.chatActivity=null; and go back to MainActivity

Use chatManager.connection.getUserID(), chatManager.connection.getNumberOfUsers(), chatManager.connection.isServer() to display

  

  

At bottom create the text area as shown in image, not necessary colors and look and feel as shown.

Place two buttons on top <"Alert Message"> and <logo "Attach File"> and a send button on right side. Move this section on top when keyboard activates and move down when keyboard deactivates

When user clicks Attach File ask for file and then in byte[] sets first element to length of file name including extension, store name of file then and then store file data.

When creating ChatActivity also transfer ChatManager object from MainActivity to ChatActivity, and whenever user clicks Send call the function chatManager.connection.sendData(ChatMsgType chatMsgType , String username, String message, byte[] fileData);

fileData is null for ChatMsgType.SIMPLE_CHAT and ChatMsgType.ALERT

  

In middle section, add chatmessage of 4 types which is mention below.

  

make a function showInfomation(String information) to show information in Toast

make a function updateStatus(int userID, boolean isServer, int numberOfUser) to update the top bar elements

make a function addMessage(DisplayMessageType, String username, String message, byte[] fileData)

For DisplayMessageType to be

JOINED, add message " -> "+username+" joined the chat!" in green color with username in dark green color

LEFT, add message " -> "+username+" left the chat!" in red color with username in dark red color

SIMPLE_CHAT add message username and message

ALERT_CHAT add message username and message with a red color text and red box border

FILE (only case when byte[] is not null) add message username, message and a button "Download "+filename which on clicking ask user to download location and downloads the file.

  

mention time on right top side using the epoachSeconds variable

  

  

ChatMsg.java, this is the file that will be transferred via ObjectOutputStream in my code

package com.thefunone.chatwm;

  

import java.io.Serializable;

  

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

epochSeconds = System.currentTimeMillis() / 1000;

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

}

  

  

  

Ask/Suggest some changes
```


------
```
Make it work for both dark and white theme based on system settings, go through each and every element on MainActivity and ChatActivity and make it work for both dark and white theme.. Also, remove the Create button and it's functionality to create new keystore file.

also, let user type in the field of Choose Key Store File
```

-------

```
Remove the Create Button for creating new KeyStore, and add a button "Select Default KeyStore File".

Dark mode is only application on eye button, make it working on all the rest elements.

Give the changed codes only.
```

------------

```
if user presses back and close the chat activity, call the function exitChat()
```

