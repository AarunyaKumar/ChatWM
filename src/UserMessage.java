import java.io.Serializable;

public class UserMessage implements Serializable {
    public String username;
    public String message;

    public UserMessage(String username, String message) {
        this.username = username;
        this.message = message;
    }
}
