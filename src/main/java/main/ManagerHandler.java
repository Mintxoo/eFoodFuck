package main;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;

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
                    case ADD_RESTAURANT -> {
                        Restaurant r = (Restaurant) msg.getPayload();
                        master.addRestaurant(r);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case ADD_PRODUCT -> {
                        ProductAction pa = (ProductAction) msg.getPayload();
                        master.addProduct(pa);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case REMOVE_PRODUCT -> {
                        ProductAction pa2 = (ProductAction) msg.getPayload();
                        master.removeProduct(pa2);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case RATE -> {
                        Rating rt = (Rating) msg.getPayload();
                        master.rateRestaurant(rt);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case REPORT -> {
                        String type = (String) msg.getPayload();
                        List<MapResult> partials = master.dispatchSalesReports(type);
                        ReduceResult rr = master.reduce(partials);
                        Map<String,Integer> ventas = rr.getVentasPorKey();
                        oos.writeObject(new Message(Message.MessageType.RESULT, ventas));
                    }
                    default -> {
                        oos.writeObject(new Message(
                                Message.MessageType.RESULT,
                                "ERROR: command not recognized"
                        ));
                    }
                }
                oos.flush();
            }
        } catch (EOFException eof) {
            // Manager closed connection
        } catch (Exception e) {
            System.err.println("Error in ManagerHandler: " + e);
        }
    }
}