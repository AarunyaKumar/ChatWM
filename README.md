# ChatWM

- Made a de-centralize chat system for Desktop Application and Android App. 
- Uses TCP and TLS System.
- Establishes network on the entered IP Address and Port Number.
- Different Security keys can be used by different groups of people using the software.
- Person can send simple messages, alert messages and files (less than 200 MB).
- If a server goes off, a new server will Re-establish.

# Security
### Version 1
- Implemented TCP connection on any IP Address and Port Number over the network. It sends data in general Java Object. It first serializes the object and then sends over the network and then de-serializes it.
##### Demerits
- Third-party software can easily see the data going through or crash the entire system by sending some garbage data.
- You can use the HackChatWM software, as provided, to extract the data and crash this system.
##### Demo Video
- Terminal based ChatWM: https://youtu.be/suu414vSLDo
- HackChatWM: https://youtu.be/x8_FtvaE33w

### Version 2
- Implemented TLS security layer which secures the network. So that no third party can see the data stream.
- Made a general java package for TCP with TLS network which can be used to in different software for like sending Chat Messages, Game Data, etc. And can be used on cross platforms like Desktop GUI and Android apps.
##### Demo Video
- TLS Implementation: https://youtu.be/ltbfwmhgybM

### Version 3
- Made a Desktop version with previous networking package.
- You can send simple messages, alert messages and files (of size below 200MB) over the network.
- As data are transmitted via serialization, Insecure De-serializable vulnerability exists. If somehow hacker gets keystore file along with password, they can send malicious code and run them over the connected devices. This can be tested by HackChatWM, you can send virus, which is just simple two like of code to give warning and open calculator app, using the same keystore file and password.
- Improved the package to avoid any third party object code to execute and fixed the Insecure De-serialization vulnerability.
##### Demerits
- Files are loaded in the memory, that's why I had to limit to 200MB. Files should be sended part by part from storage to network. (For this, either there should be a centralized server, or there must be a mutual acceptance between devices to simultaneously send and receive data.)
##### Demo Video
- ChatWM Desktop: https://youtu.be/_1x76ojISVE
- ChatWM Insecure Deserialization: https://youtu.be/XklyLN2ohoQ

### Version 4
- Made an Android app using the previous made networking package. 
##### Demerits
- Currently, few features still needs to be fixed, like when android app becomes server, it does not allow clients to connect.
- File names, should be sended over network correctly.
##### Demo Video
- Desktop + Android App: https://youtu.be/QEhMjsGsU0A


## Use of AI in this Projects
I used AI in the development of Windows Graphical UI and in Android app UI as coding myself would have taken atleast 7-14 days.
You can check the prompts as mentioned in the "AI Prompts Win GUI.md" and "AI Prompts for Android App.md" files.


## Practical Use case
- Can be used in hospital system for LAN based network with a de-centralized server, to send messages and files of patient record.
- Can be used by enterprises for LAN based network.
- The Java Package can be used for many Java based projects involving TCP and TLS connection to send any kind of data.