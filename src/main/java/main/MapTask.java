// src/main/java/main/MapTask.java
package main;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

public class MapTask {
    private final FilterSpec filters;
    private final WorkerInfo targetWorker;

    public MapTask(FilterSpec filters, WorkerInfo targetWorker) {
        this.filters = Objects.requireNonNull(filters);
        this.targetWorker = Objects.requireNonNull(targetWorker);
    }

    /** Envía un Message(TASK, FilterSpec) al Worker y recibe Message(RESULT, MapResult). */
    public MapResult execute() throws IOException, ClassNotFoundException {
        try (Socket sock = new Socket(targetWorker.getHost(), targetWorker.getPort());
             ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream  ois = new ObjectInputStream(sock.getInputStream())) {
            
            oos.writeObject(new Message(Message.MessageType.TASK, filters));
            Message resp = (Message) ois.readObject();
            if (resp.getType() != Message.MessageType.RESULT) {
                throw new IOException("Esperado RESULT, vino " + resp.getType());
            }
            return (MapResult) resp.getPayload();
        }
    }
}
