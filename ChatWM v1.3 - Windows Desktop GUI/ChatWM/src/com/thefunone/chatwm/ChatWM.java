package com.thefunone.chatwm;

public class ChatWM {
    SetUpWindow setUpWindow;
    ChatWindow chatWindow;

    ChatWM(){
        setSetUpWindow();
    }

    void setSetUpWindow(){
        setUpWindow = new SetUpWindow(this);
    }


    public void connect(String username, String ipAddress, int portNum, String keystorePath, String password){
        chatWindow = new ChatWindow(this, username, ipAddress, portNum, keystorePath, password);
        chatWindow.createFrame();
    }


    public void failedToConnect(String errorMessage){
        if(setUpWindow!=null) {
            setUpWindow.resetLoading(errorMessage);
            try{
                chatWindow.chatManager.connection.close();
            }catch(Exception _){}
            chatWindow.frame.dispose();
            chatWindow = null;
        }
    }
    public void connectedSuccessfully() {
        if(setUpWindow!=null)
            setUpWindow.closeSetUp();
    }

    static void main(String[] args) {new ChatWM();}
}
