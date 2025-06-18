// src/main/java/main/ClientHandler.java
package main;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable, Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Socket socket;
    private final MasterServer master;

    public ClientHandler(Socket socket, MasterServer master) {
        this.socket = socket;
        this.master = master;
    }

    @Override
    public void run() {
        try (
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream  ois = new ObjectInputStream(socket.getInputStream());
        ) {
            Message msg;
            while ((msg = (Message) ois.readObject()) != null) {
                switch (msg.getType()) {
                    case PING -> {
                        oos.writeObject(new Message(Message.MessageType.PONG, "OK"));
                    }
                    case TASK -> {
                        FilterSpec fs = (FilterSpec) msg.getPayload();
                        List<MapResult> partials = master.dispatchMapTasks(fs);
                        ReduceResult rr = new ReduceTask().combine(partials);
                        oos.writeObject(new Message(Message.MessageType.RESULT, rr));
                    }
                    case SALE -> {
                        // reenviamos la venta a todos los Workers
                        master.broadcast(new Message(Message.MessageType.SALE, msg.getPayload()));
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    default -> {
                        oos.writeObject(new Message(Message.MessageType.RESULT, 
                                                   "ERROR: comando no soportado por ClientHandler"));
                    }
                }
                oos.flush();
            }
        } catch (EOFException eof) {
            // cliente cerró conexión
        } catch (Exception e) {
            System.err.println("Error en ClientHandler: " + e);
        }
    }
}
