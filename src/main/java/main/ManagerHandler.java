// src/main/java/main/ManagerHandler.java
package main;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ManagerHandler implements Runnable, Serializable {
    private static final long serialVersionUID = 1L;
    
    private final Socket socket;
    private final MasterServer master;

    public ManagerHandler(Socket socket, MasterServer master) {
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
                    case REGISTER -> {
                        WorkerInfo w = (WorkerInfo) msg.getPayload();
                        master.registerWorker(w);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case REPORT -> {
                        String reportType = (String) msg.getPayload();
                        List<MapResult> partials = master.dispatchSalesReports(reportType);
                        ReduceResult rr = new ReduceTask().combine(partials);
                        oos.writeObject(new Message(Message.MessageType.RESULT, rr.getVentasPorKey()));
                    }
                    default -> {
                        oos.writeObject(new Message(Message.MessageType.RESULT, 
                                                   "ERROR: comando no soportado por ManagerHandler"));
                    }
                }
                oos.flush();
            }
        } catch (EOFException eof) {
            // manager cerró conexión
        } catch (Exception e) {
            System.err.println("Error en ManagerHandler: " + e);
        }
    }
}
