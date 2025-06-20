package main;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ManagerConsole {
    private static int nextWorkerId   = 2;      // Next is id: w2
    private static int nextWorkerPort = 6002;   // Next is port: 6002

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Use: ManagerConsole <host> <port>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket sock = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Manager connected to Master in " + host + ":" + port);
            System.out.println("Commands: addRest <name> <categories> <initialRating> <priceCat>");
            System.out.println("          addProd <store> <product> <price>");
            System.out.println("          delProd <store> <product>");
            System.out.println("          salesReport food <FoodCategory>");
            System.out.println("          salesReport product <ProductCategory>");
            System.out.println("          salesReport all");
            System.out.println("          spawnWorker <masterHost> <masterPort> [workerHost]");
            System.out.println("          exit");

            String line;
            while ((line = console.readLine()) != null) {
                if (line.isBlank()) continue;
                if (line.equalsIgnoreCase("exit")) break;
                String[] parts = line.trim().split("\\s+");
                String cmd = parts[0].toLowerCase();

                System.out.println("New");

                switch (cmd) {
                    case "addrest": {
                        if (parts.length != 7) {
                            System.out.println("Use: addRest <name> <categories> <initialRating> <priceCat>");
                            continue;
                        }
                        Restaurant r = new Restaurant(
                                parts[1],
                                Double.parseDouble(parts[2]),
                                Double.parseDouble(parts[3]),
                                parts[4],
                                Double.parseDouble(parts[5]),
                                PriceCategory.valueOf(parts[6])
                        );
                        oos.writeObject(new Message(Message.MessageType.CREATE_RESTAURANT, r));
                        break;
                    }
                    case "addprod": {
                        if (parts.length != 4) {
                            System.out.println("Use: addProd <store> <product> <price>");
                            continue;
                        }
                        ProductAction pa = new ProductAction(parts[1], parts[2], Double.parseDouble(parts[3]));
                        oos.writeObject(new Message(Message.MessageType.ADD_PRODUCT, pa));
                        break;
                    }
                    case "delprod": {
                        if (parts.length != 3) {
                            System.out.println("Use: delProd <store> <product>");
                            continue;
                        }
                        ProductAction pa = new ProductAction(parts[1], parts[2], 0.0);
                        oos.writeObject(new Message(Message.MessageType.REMOVE_PRODUCT, pa));
                        break;
                    }
                    case "salesreport": {
                        String reportType;
                        if (parts.length == 2 && "all".equals(parts[1])) {
                            reportType = "all";
                        } else if (parts.length == 3 && ("food".equals(parts[1]) || "product".equals(parts[1]))) {
                            reportType = parts[1] + ":" + parts[2];
                        } else {
                            System.out.println("Use: salesReport food <FoodCategory> | product <ProductCategory> | all");
                            continue;
                        }
                        oos.writeObject(new Message(Message.MessageType.REPORT, reportType));
                        oos.flush();

                        Message resp = (Message) ois.readObject();
                        Map<String, Integer> salesMap = (Map<String, Integer>) resp.getPayload();

                        if ("all".equals(parts[1])) {
                            System.out.println("Master> " + salesMap);
                        } else {
                            Map<String, Integer> filteredSales = new LinkedHashMap<>();
                            int total = 0;

                            for (Map.Entry<String, Integer> entry : salesMap.entrySet()) {
                                if (!entry.getKey().equalsIgnoreCase("total")) {
                                    filteredSales.put(entry.getKey(), entry.getValue());
                                    total += entry.getValue();
                                }
                            }

                            filteredSales.put("total", total);
                            System.out.println("Master> " + filteredSales);
                        }
                        continue;
                    }
                    case "spawnworker": {

                        if (parts.length < 3) {
                            System.out.println("Use: spawnWorker <masterHost> <masterPort> [workerHost]");
                            break;
                        }

                        String masterHost = parts[1];
                        int masterPort;
                        try {
                            masterPort = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid Master Port: " + parts[2]);
                            break;
                        }

                        String workerHost = (parts.length > 3) ? parts[3] : "localhost";

                        String workerId = "w" + nextWorkerId++;
                        int workerPort  = nextWorkerPort++;

                        new Thread(() -> {
                            WorkerInfo info = new WorkerInfo(workerId, workerHost, workerPort);
                            try {
                                new WorkerNode(info).start(masterHost, masterPort);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }).start();
                        continue;
                    }
                    default:
                        System.out.println("Unknown command: " + cmd);
                        continue;
                }

                oos.flush();
                Message resp = (Message) ois.readObject();
                System.out.println("Master> " + resp.getPayload());
            }
            System.out.println("Exiting...");
        }
    }
}
