# ChatWM

- Made a chat system in Desktop Application and Android app. 
- Uses TCP and TLS system
- Person can send simple messages, alert messages and files.
- If a server goes off, a new server will re-establish

# Security
### Version 1
- Implemented TCP connection over the network. It sends data in Object. It first serialize the object and then sends over the network and then de-serialize it.

###### Demerits
- Third-party software can easily see the data going through or crash the entire system.
- You can use the HackChatWM software to extract the data and crash this system.

### Version 2
Implemented TLS security layer which secures the network. So that no third party can see the data stream.
Made a general java package for TCP with TLS network which can be used to in different software for like sending Chat Messages, Game Data, etc. And can be used on cross platforms like Desktop GUI and Android apps.

### Version 3
- Made a Desktop version with previous networking package.
- You can send simple messages, alert messages and files (of size below 200MB) over the network.
- As data are transmitted via serialization, Insecure De-serializable vulnerable exists. If somehow hacker gets keystore file along with password, they can send malicious code and run them over the connected devices. This can be tested by HackChatWM, you can send virus, which is just simple two like of code to give warning and open calculator app, using the same keystore file and password.
- Improved the package to avoid any third party object code to execute.

##### Demerits
- Files are loaded in the memory, that's why I had to limit to 200MB. Files should be sended part by part from storage to network.

### Version 4
- Made an Android app using the previous made networking package. 

##### Demerits
- Currently, few features still needs to be fixed, like when android app becomes server, it does not allow clients to connect.
- File names, should be sended over network correctly.


## Use of AI in this Projects
I used AI in the development of Windows Graphical UI and in Android app UI as coding myself would have taken atleast 7-14 days.
You can check the prompts as mentioned in the files.

## Practical Use case
- Can be used in hospital system for LAN based network with a de-centralized server.
- Can be used by enterprises for LAN based network.
- The Java Package can be used for many Java based projects involving TCP and TLS connection.