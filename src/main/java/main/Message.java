package main;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        PING, PONG,
        REGISTER,
        TASK, RESULT,
        SALE, REPORT,
        ADD_RESTAURANT, ADD_PRODUCT, REMOVE_PRODUCT, RATE, REMOVE_RESTAURANT,
        CREATE_RESTAURANT
    }

    private MessageType type;
    private Object payload;

    public Message() {}
    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
    public MessageType getType() { return type; }
    public Object getPayload() { return payload; }

    @Override
    public String toString() {
        return "Message{" + "type=" + type + ", payload=" + payload + '}';
    }
}