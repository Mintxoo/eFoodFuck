// src/main/java/main/ReportTask.java
package main;

import java.io.*;
import java.net.Socket;

public class ReportTask {
    private final String reportType;
    private final WorkerInfo targetWorker;

    public ReportTask(String reportType, WorkerInfo targetWorker) {
        this.reportType = reportType;
        this.targetWorker = targetWorker;
    }

    /** Envía Message(REPORT, String) y recibe Message(RESULT, MapResult). */
    public MapResult execute() throws IOException, ClassNotFoundException {
        try (Socket sock = new Socket(targetWorker.getHost(), targetWorker.getPort());
             ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream  ois = new ObjectInputStream(sock.getInputStream())) {
            
            oos.writeObject(new Message(Message.MessageType.REPORT, reportType));
            Message resp = (Message) ois.readObject();
            if (resp.getType() != Message.MessageType.RESULT) {
                throw new IOException("Esperado RESULT, vino " + resp.getType());
            }
            return (MapResult) resp.getPayload();
        }
    }
}
