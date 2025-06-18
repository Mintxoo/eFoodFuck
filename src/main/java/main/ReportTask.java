package main;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.Map;
import java.util.List;

/**
 * Envía un REPORT al Worker y recibe un MapResult con ventas.
 */
public class ReportTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String reportType;
    private final WorkerInfo targetWorker;

    public ReportTask(String reportType, WorkerInfo targetWorker) {
        this.reportType = Objects.requireNonNull(reportType);
        this.targetWorker = Objects.requireNonNull(targetWorker);
    }

    public MapResult execute() throws IOException, ClassNotFoundException {
        try (Socket s = new Socket(targetWorker.getHost(), targetWorker.getPort());
             ObjectOutputStream oos = new ObjectOutputStream(s.getOutputStream());
             ObjectInputStream  ois = new ObjectInputStream(s.getInputStream())) {

            oos.writeObject(new Message(Message.MessageType.REPORT, reportType));
            oos.flush();

            Message resp = (Message) ois.readObject();
            if (resp.getType() != Message.MessageType.RESULT) {
                throw new IOException("ReportTask: respuesta inesperada: " + resp.getType());
            }
            // Payload puede ser Map<String,Integer> or MapResult; adaptamos
            Object payload = resp.getPayload();
            if (payload instanceof MapResult) {
                return (MapResult) payload;
            } else if (payload instanceof Map) {
                // Convertimos Map<String,Integer> en MapResult
                MapResult mr = new MapResult();
                @SuppressWarnings("unchecked")
                Map<String,Integer> ventas = (Map<String,Integer>) payload;
                ventas.forEach(mr::addVenta);
                return mr;
            } else {
                throw new IOException("ReportTask: tipo de payload inesperado: " + payload.getClass());
            }
        }
    }
}