package main;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * CLI sencilla para enviar comandos al Master.
 */
public class ClientConsole {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Uso: ClientConsole <host> <port>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket sock = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Conectado a Master en " + host + ":" + port);
            System.out.println("Comandos: ping, search <lat> <lon> <cats> <minStars> <priceCat>,");
            System.out.println("          buy <store> <item:qty,...>, rate <store> <stars>, exit");

            String line;
            while ((line = console.readLine()) != null) {
                if (line.isBlank()) continue;
                if (line.equalsIgnoreCase("exit")) break;
                String[] tokens = line.trim().split("\\s+");
                String cmd = tokens[0].toLowerCase();

                switch (cmd) {
                    case "ping":
                        oos.writeObject(new Message(Message.MessageType.PING, null));
                        break;

                    case "search": {
                        if (tokens.length < 6) {
                            System.out.println("Uso: search <lat> <lon> <cats> <minStars> <priceCat>");
                            continue;
                        }
                        double lat = Double.parseDouble(tokens[1]);
                        double lon = Double.parseDouble(tokens[2]);
                        String[] cats = tokens[3].split(",");
                        Set<String> catSet = new HashSet<>(Arrays.asList(cats));
                        int minStars = Integer.parseInt(tokens[4]);
                        PriceCategory pc = PriceCategory.valueOf(tokens[5]);
                        FilterSpec fs = new FilterSpec(lat, lon, catSet, minStars, pc);
                        oos.writeObject(new Message(Message.MessageType.TASK, fs));
                        break;
                    }

                    case "buy": {
                        if (tokens.length < 3) {
                            System.out.println("Uso: buy <store> <item:qty,...>");
                            continue;
                        }
                        String store = tokens[1];
                        String[] items = tokens[2].split(",");
                        Map<String, Integer> map = new HashMap<>();
                        for (String it : items) {
                            String[] kv = it.split(":");
                            map.put(kv[0], Integer.parseInt(kv[1]));
                        }
                        Sale sale = new Sale(store, map);
                        oos.writeObject(new Message(Message.MessageType.SALE, sale));
                        break;
                    }

                    case "rate": {
                        if (tokens.length < 3) {
                            System.out.println("Uso: rate <store> <stars>");
                            continue;
                        }
                        String store = tokens[1];
                        int stars = Integer.parseInt(tokens[2]);
                        Rating rating = new Rating(store, stars);
                        oos.writeObject(new Message(Message.MessageType.RATE, rating));
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
