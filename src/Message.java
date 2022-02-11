import java.io.Serializable;

public class Message implements Serializable {
    private String userId;
    private String chat;

    public Message() {
    }

    public Message(String userId, String chat) {
        this.userId = userId;
        this.chat = chat;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChat() {
        return chat;
    }

    public void setChat(String chat) {
        this.chat = chat;
    }

    @Override
    public String toString() {
        return "Message{" +
                "userId='" + userId + '\'' +
                ", chat='" + chat + '\'' +
                '}';
    }
}
