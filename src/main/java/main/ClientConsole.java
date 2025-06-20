package main;

import java.io.*;
import java.net.Socket;
import java.util.*;


public class ClientConsole {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Use: ClientConsole <host> <port>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try (Socket sock = new Socket(host, port);
             ObjectOutputStream oos = new ObjectOutputStream(sock.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(sock.getInputStream());
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to Master in " + host + ":" + port);
            System.out.println("Commands: ping,");
            System.out.println("          search <cats> - <minStars> - [priceCat] (pc optional),");
            System.out.println("          buy <store> <item:qty,...>, rate <store> <stars>, exit");

            final double DEFAULT_LAT = 40.01;
            final double DEFAULT_LON = 23.01;

            String line;
            while ((line = console.readLine()) != null) {
                if (line.isBlank()) continue;
                if (line.equalsIgnoreCase("exit")) break;
                String[] parts = line.trim().split("\\s+", 2);
                String cmd = parts[0].toLowerCase();

                switch (cmd) {
                    case "ping":
                        oos.writeObject(new Message(Message.MessageType.PING, null));
                        break;

                    case "search": {
                        if (parts.length < 2) {
                            System.out.println("Use: search <cats> - <minStars> - [priceCat] (pc optional)");
                            continue;
                        }
                        String[] fields = parts[1].split("\s*-\s*", -1);

                        if (fields.length < 2 || fields.length > 3) {
                            System.out.println("Uncompleted search, its needed from 2 to 3 fields separated by '-'");
                            continue;
                        }

                        Set<String> catSet = new HashSet<>();
                        if (!fields[0].isEmpty()) {
                            catSet = new HashSet<>(Arrays.asList(fields[0].split(",")));
                        }
                        Integer minStars = null;
                        if (!fields[1].isEmpty()) {
                            minStars = Integer.parseInt(fields[1]);
                        }
                        PriceCategory pc = null;
                        if (fields.length == 3 && !fields[2].isEmpty()) {
                            pc = PriceCategory.valueOf(fields[2]);
                        }

                        FilterSpec fs = new FilterSpec(
                                DEFAULT_LAT,
                                DEFAULT_LON,
                                catSet,
                                minStars != null ? minStars : 0,
                                pc
                        );
                        oos.writeObject(new Message(Message.MessageType.TASK, fs));
                        break;
                    }

                    case "buy": {
                        if (parts.length < 2) {
                            System.out.println("Use: buy <store> <item:qty,...>");
                            continue;
                        }
                        String[] tokens = parts[1].split("\\s+", 2);
                        String store = tokens[0];
                        Map<String, Integer> map = new HashMap<>();
                        if (tokens.length > 1 && !tokens[1].isEmpty()) {
                            for (String it : tokens[1].split(",")) {
                                String[] kv = it.split(":");
                                map.put(kv[0], Integer.parseInt(kv[1]));
                            }
                        }
                        Sale sale = new Sale(store, map);
                        oos.writeObject(new Message(Message.MessageType.SALE, sale));
                        break;
                    }

                    case "rate": {
                        if (parts.length < 2) {
                            System.out.println("Use: rate <store> <stars>");
                            continue;
                        }
                        String[] tokens = parts[1].split("\\s+");
                        if (tokens.length < 2) {
                            System.out.println("Use: rate <store> <stars>");
                            continue;
                        }
                        String store = tokens[0];
                        int stars = Integer.parseInt(tokens[1]);
                        Rating rating = new Rating(store, stars);
                        oos.writeObject(new Message(Message.MessageType.RATE, rating));
                        break;
                    }

                    default:
                        System.out.println("Unexpected command: " + cmd);
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
