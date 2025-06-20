package main;

import java.io.*;
import java.net.Socket;
import java.util.*;


public class TestMain {
    public static void main(String[] args) throws Exception {
        final int masterPort = 5555;
        final WorkerInfo wi = new WorkerInfo("w1", "localhost", 6001);

        MasterServer master = new MasterServer(masterPort);
        new Thread(() -> {
            try { master.start(); }
            catch (Exception e) { e.printStackTrace(); }
        }, "MasterThread").start();

        WorkerNode worker = new WorkerNode(wi);
        new Thread(() -> {
            try { worker.start("localhost", masterPort); }
            catch (Exception e) { e.printStackTrace(); }
        }, "WorkerThread").start();

        Thread.sleep(1000);

        try (Socket client = new Socket("localhost", masterPort);
             ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream  ois = new ObjectInputStream(client.getInputStream())) {

            System.out.println("--- ADD_RESTAURANT ---");
            Restaurant r = new Restaurant(
                    "PizzaFun", 40.01, 23.01,
                    "pizza", 5, PriceCategory.TWO_DOLLARS
            );
            oos.writeObject(new Message(Message.MessageType.ADD_RESTAURANT, r));
            oos.flush();
            System.out.println("Master> " + ois.readObject());

            System.out.println("\n--- TASK ---");
            FilterSpec fs = new FilterSpec(
                    40.0, 23.0,
                    new HashSet<>(Arrays.asList("pizza")),
                    4, PriceCategory.TWO_DOLLARS
            );
            oos.writeObject(new Message(Message.MessageType.TASK, fs));
            oos.flush();
            Message taskResp = (Message) ois.readObject();
            System.out.println("Master> " + taskResp.getPayload());

            System.out.println("\n--- SALE ---");
            Map<String,Integer> items = Map.of("PizzaFun", 3);
            Sale sale = new Sale("PizzaFun", items);
            oos.writeObject(new Message(Message.MessageType.SALE, sale));
            oos.flush();
            System.out.println("Master> " + ois.readObject());

            System.out.println("\n--- REPORT (food) ---");
            oos.writeObject(new Message(Message.MessageType.REPORT, "food"));
            oos.flush();
            Message repResp = (Message) ois.readObject();
            System.out.println("Master> " + repResp.getPayload());

            System.out.println("\n--- RATE ---");
            Rating rating = new Rating("PizzaFun", 4);
            oos.writeObject(new Message(Message.MessageType.RATE, rating));
            oos.flush();
            System.out.println("Master> " + ois.readObject());

            System.out.println("\n--- TASK after RATE ---");
            oos.writeObject(new Message(Message.MessageType.TASK, fs));
            oos.flush();
            Message afterRate = (Message) ois.readObject();
            System.out.println("Master> " + afterRate.getPayload());

        }

        System.out.println("\n=== Integration Test Finished ===");
        System.exit(0);
    }
}
