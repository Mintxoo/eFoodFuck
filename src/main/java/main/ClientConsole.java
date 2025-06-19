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
            System.out.println("Comandos: ping, search <lat> - <lon> - <cats> - <minStars> - <priceCat> (pc opcional),");
            System.out.println("          buy <store> <item:qty,...>, rate <store> <stars>, exit");

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
                            System.out.println("Uso: search <lat> - <lon> - <cats> - <minStars> - <priceCat> (pc opcional)");
                            continue;
                        }
                        // split con - permitiendo campos vacíos y trailing
                        String[] fields = parts[1].split("\\s*-\\s*", -1);
                        if (fields.length < 4 || fields.length > 5) {
                            System.out.println("Busq. incompleta, se necesitan de 4 a 5 campos separados por '-'");
                            continue;
                        }
                        // asignar con chequeo de índices
                        Double lat = null;
                        Double lon = null;
                        Set<String> catSet = new HashSet<>();
                        Integer minStars = null;
                        PriceCategory pc = null;
                        if (fields.length >= 1 && !fields[0].isEmpty()) lat = Double.parseDouble(fields[0]);
                        if (fields.length >= 2 && !fields[1].isEmpty()) lon = Double.parseDouble(fields[1]);
                        if (fields.length >= 3 && !fields[2].isEmpty()) catSet = new HashSet<>(Arrays.asList(fields[2].split(",")));
                        if (fields.length >= 4 && !fields[3].isEmpty()) minStars = Integer.parseInt(fields[3]);
                        if (fields.length == 5 && !fields[4].isEmpty()) pc = PriceCategory.valueOf(fields[4]);

                        // Construir FilterSpec permitiendo priceCategory nulo
                        FilterSpec fs = new FilterSpec(
                                lat != null ? lat : 0.0,
                                lon != null ? lon : 0.0,
                                catSet,
                                minStars != null ? minStars : 0,
                                pc // si es null, en WorkerNode skipPrice = true
                        );
                        oos.writeObject(new Message(Message.MessageType.TASK, fs));
                        break;
                    }

                    case "buy": {
                        if (parts.length < 2) {
                            System.out.println("Uso: buy <store> <item:qty,...>");
                            continue;
                        }
                        String[] tokens = parts[1].split("\\s+", 2);
                        String store = tokens[0];
                        Map<String, Integer> map = new HashMap<>();
                        if (tokens.length > 1) {
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
                            System.out.println("Uso: rate <store> <stars>");
                            continue;
                        }
                        String[] tokens = parts[1].split("\\s+");
                        if (tokens.length < 2) {
                            System.out.println("Uso: rate <store> <stars>");
                            continue;
                        }
                        String store = tokens[0];
                        int stars = Integer.parseInt(tokens[1]);
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
