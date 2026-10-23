import java.io.Serializable;
import java.time.Instant;


public class ChatMsg implements Serializable {
    String username, message;
    long epochSeconds;

    ChatMsg(String username, String message) {
        this.username = username;
        this.message = message;
        epochSeconds = Instant.now().getEpochSecond();
    }
    long getEpochSeconds(){
        return epochSeconds;
    }
    String getUsername(){
        return username;
    }
}
