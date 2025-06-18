// src/main/java/main/Message.java
package main;

import java.io.Serializable;

public class Message implements Serializable {
    public enum MessageType { PING, PONG, TASK, RESULT, REGISTER, SALE, REPORT }

    private final MessageType type;
    private final Object payload;        // puede ser String, FilterSpec, MapResult, WorkerInfo…

    public Message(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
    public MessageType getType()   { return type; }
    public Object      getPayload(){ return payload; }
}
