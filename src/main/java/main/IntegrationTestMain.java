package main;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Prueba de integración: arranca MasterServer y WorkerNode en hilos,
 * luego simula un cliente que envía ADD_RESTAURANT, TASK, SALE, REPORT y RATE.
 */
public class IntegrationTestMain {
    public static void main(String[] args) throws Exception {
        // Puerto del master y worker
        final int masterPort = 5555;
        final WorkerInfo wi = new WorkerInfo("w1", "localhost", 6001);

        // 1) Arrancar MasterServer en hilo
        MasterServer master = new MasterServer(masterPort);
        new Thread(() -> {
            try { master.start(); }
            catch (Exception e) { e.printStackTrace(); }
        }, "MasterThread").start();

        // 2) Arrancar WorkerNode en hilo
        WorkerNode worker = new WorkerNode(wi);
        new Thread(() -> {
            try { worker.start("localhost", masterPort); }
            catch (Exception e) { e.printStackTrace(); }
        }, "WorkerThread").start();

        // Pequeña pausa para asegurar registro
        Thread.sleep(1000);

        // 3) Simular cliente
        try (Socket client = new Socket("localhost", masterPort);
             ObjectOutputStream oos = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream  ois = new ObjectInputStream(client.getInputStream())) {

            // a) ADD_RESTAURANT
            System.out.println("--- ADD_RESTAURANT ---");
            Restaurant r = new Restaurant(
                    "PizzaFun", 40.01, 23.01,
                    "pizza", 5, PriceCategory.TWO_DOLLARS
            );
            oos.writeObject(new Message(Message.MessageType.ADD_RESTAURANT, r));
            oos.flush();
            System.out.println("Master> " + ois.readObject());

            // b) TASK
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

            // c) SALE
            System.out.println("\n--- SALE ---");
            Map<String,Integer> items = Map.of("PizzaFun", 3);
            Sale sale = new Sale("PizzaFun", items);
            oos.writeObject(new Message(Message.MessageType.SALE, sale));
            oos.flush();
            System.out.println("Master> " + ois.readObject());

            // d) REPORT
            System.out.println("\n--- REPORT (food) ---");
            oos.writeObject(new Message(Message.MessageType.REPORT, "food"));
            oos.flush();
            Message repResp = (Message) ois.readObject();
            System.out.println("Master> " + repResp.getPayload());

            // e) RATE
            System.out.println("\n--- RATE ---");
            Rating rating = new Rating("PizzaFun", 4);
            oos.writeObject(new Message(Message.MessageType.RATE, rating));
            oos.flush();
            System.out.println("Master> " + ois.readObject());

            // f) TASK tras rating
            System.out.println("\n--- TASK after RATE ---");
            oos.writeObject(new Message(Message.MessageType.TASK, fs));
            oos.flush();
            Message afterRate = (Message) ois.readObject();
            System.out.println("Master> " + afterRate.getPayload());

        }

        // Final
        System.out.println("\n=== Integration Test Finished ===");
        System.exit(0);
    }
}
