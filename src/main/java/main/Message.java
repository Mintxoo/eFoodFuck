package main;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum MessageType {
        PING, PONG,
        REGISTER, // Manager registra Worker
        TASK, RESULT, // Client busca
        SALE, REPORT, // Client compra + Manager reportes
        ADD_RESTAURANT, ADD_PRODUCT, REMOVE_PRODUCT, RATE // Funciones Manager Mode
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