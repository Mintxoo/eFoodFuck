package main;

import java.io.*;
import java.net.Socket;
import java.util.Objects;


public class MapTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private final FilterSpec filters;
    private final WorkerInfo targetWorker;

    public MapTask(FilterSpec filters, WorkerInfo targetWorker) {
        this.filters = Objects.requireNonNull(filters);
        this.targetWorker = Objects.requireNonNull(targetWorker);
    }

    public MapResult execute() throws IOException, ClassNotFoundException {
        try (Socket s = new Socket(targetWorker.getHost(), targetWorker.getPort());
             ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream  ois = new ObjectInputStream(s.getInputStream())) {

            oos.writeObject(new Message(Message.MessageType.TASK, filters));
            oos.flush();

            Message resp = (Message) ois.readObject();
            if (resp.getType() != Message.MessageType.RESULT) {
                throw new IOException("MapTask: unexpected response: " + resp.getType());
            }
            return (MapResult) resp.getPayload();
        }
    }
}