import java.io.Serializable;
import java.time.Instant;

enum MsgType{
    CHAT,
    JOINED,
    LEFT,
    UID_GIVER,
    CLIENT_STATUS
}

public class UserMessage implements Serializable {
    public String username;
    public String message;
    public MsgType msgType;
    public long epochSeconds;

    public UserMessage(MsgType msgType, String username, String message) {
        this.msgType = msgType;
        this.username = username;
        this.message = message;
        epochSeconds = Instant.now().getEpochSecond();
    }
}
