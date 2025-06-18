package main;

import java.io.*;
import java.net.Socket;
import java.util.Objects;

/**
 * Envía un TASK al Worker y recibe un MapResult.
 */
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

            // Enviamos el mensaje TASK con el FilterSpec
            oos.writeObject(new Message(Message.MessageType.TASK, filters));
            oos.flush();

            // Leemos la respuesta
            Message resp = (Message) ois.readObject();
            if (resp.getType() != Message.MessageType.RESULT) {
                throw new IOException("MapTask: respuesta inesperada: " + resp.getType());
            }
            return (MapResult) resp.getPayload();
        }
    }
}