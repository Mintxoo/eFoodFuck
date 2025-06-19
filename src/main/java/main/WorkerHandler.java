package main;

import java.io.*;
import java.net.Socket;

public class WorkerHandler implements Runnable {
    private final Socket socket;
    private final WorkerNode worker;

    public WorkerHandler(Socket socket, WorkerNode worker) {
        this.socket = socket;
        this.worker = worker;
    }

    @Override
    public void run() {
        try (
                socket;
                ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        ) {
            Message msg;
            while ((msg = (Message) ois.readObject()) != null) {
                System.out.println("WorkerHandler recibe: " + msg.getType() + " con payload=" + msg.getPayload());
                switch (msg.getType()) {
                    case ADD_RESTAURANT -> {
                        worker.addRestaurant((Restaurant) msg.getPayload());
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case ADD_PRODUCT -> {
                        ProductAction pa = (ProductAction) msg.getPayload();
                        worker.addProduct(pa.getStoreName(), pa.getProductName(), pa.getPrice());
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case REMOVE_PRODUCT -> {
                        ProductAction pa2 = (ProductAction) msg.getPayload();
                        worker.removeProduct(pa2.getStoreName(), pa2.getProductName());
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case RATE -> {
                        Rating rt = (Rating) msg.getPayload();
                        worker.rate(rt.getStoreName(), rt.getStars());
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                    }
                    case TASK -> {
                        FilterSpec fs = (FilterSpec) msg.getPayload();
                        MapResult mr = worker.handleSearch(fs);
                        oos.writeObject(new Message(Message.MessageType.RESULT, mr));
                    }
                    case SALE -> {
                        Sale sale = (Sale) msg.getPayload();
                        worker.handleSale(sale);
                        oos.writeObject(new Message(Message.MessageType.RESULT, "OK"));
                        oos.flush();
                    }

                    case REPORT -> {
                        String type = (String) msg.getPayload();
                        MapResult mr = worker.handleReport(type);
                        oos.writeObject(new Message(Message.MessageType.RESULT, mr));
                    }
                    case PING -> {
                        oos.writeObject(new Message(Message.MessageType.PONG, "OK"));
                    }
                    default -> {
                        oos.writeObject(new Message(Message.MessageType.RESULT, "UNKNOWN"));
                    }
                }
                oos.flush();
            }
        } catch (EOFException eof) {
            // cierre normal
        } catch (Exception e) {
            System.err.println("Error en WorkerHandler: " + e);
        }
    }
}
