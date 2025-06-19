package main;

import java.io.*;
import java.net.Socket;
import java.util.*;

public class ManagerConsole {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: ManagerConsole <host> <port>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket sock = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Manager conectado a Master en " + host + ":" + port);
            System.out.println("Comandos: addRest <nombre> <lat> <lon> <categoria> <ratingInicial> <priceCat>");
            System.out.println("          addProd <store> <producto> <precio>");
            System.out.println("          delProd <store> <producto>");
            System.out.println("          salesReport");
            System.out.println("          exit");

            String line;
            while ((line = console.readLine()) != null) {
                if (line.isBlank()) continue;
                if (line.equalsIgnoreCase("exit")) break;
                String[] parts = line.trim().split("\\s+");
                String cmd = parts[0].toLowerCase();

                switch (cmd) {
                    case "addrest": {
                        if (parts.length != 7) {
                            System.out.println("Uso: addRest <nombre> <lat> <lon> <categoria> <ratingInicial> <priceCat>");
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
                        oos.writeObject(new Message(Message.MessageType.ADD_RESTAURANT, r));
                        break;
                    }
                    case "addprod": {
                        if (parts.length != 4) {
                            System.out.println("Uso: addProd <store> <producto> <precio>");
                            continue;
                        }
                        ProductAction pa = new ProductAction(parts[1], parts[2], Double.parseDouble(parts[3]));
                        oos.writeObject(new Message(Message.MessageType.ADD_PRODUCT, pa));
                        break;
                    }
                    case "delprod": {
                        if (parts.length != 3) {
                            System.out.println("Uso: delProd <store> <producto>");
                            continue;
                        }
                        ProductAction pa = new ProductAction(parts[1], parts[2], 0.0);
                        oos.writeObject(new Message(Message.MessageType.REMOVE_PRODUCT, pa));
                        break;
                    }
                    case "salesreport": {
                        oos.writeObject(new Message(Message.MessageType.REPORT, "product"));
                        break;
                    }
                    default:
                        System.out.println("Comando desconocido: " + cmd);
                        continue;
                }

                oos.flush();
                Message resp = (Message) ois.readObject();
                System.out.println("Master> " + resp.getPayload());
            }
            System.out.println("Saliendo...");
        }
    }
}

