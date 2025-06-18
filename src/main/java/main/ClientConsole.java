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
             ObjectInputStream  ois = new ObjectInputStream(sock.getInputStream());
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Conectado a Master en " + host + ":" + port);
            System.out.println("Comandos: ping, search <lat> <lon> <cats> <minStars> <priceCat>,");
            System.out.println("          buy <store> <item:qty,...>, rate <store> <stars>, exit");

            String line;
            while ((line = console.readLine()) != null) {
                if (line.isBlank()) continue;
                if (line.equalsIgnoreCase("exit")) break;
                String[] parts = line.split(" ", 3);
                String cmd = parts[0].toLowerCase();

                switch(cmd) {
                    case "ping":
                        oos.writeObject(new Message(Message.MessageType.PING, null));
                        break;
                    case "search": {
                        String[] p = parts[1].split(" ");
                        double lat = Double.parseDouble(p[0]);
                        double lon = Double.parseDouble(p[1]);
                        String[] cats = p[2].split(",");
                        Set<String> catSet = new HashSet<>(Arrays.asList(cats));
                        int minStars = Integer.parseInt(p[3]);
                        PriceCategory pc = PriceCategory.valueOf(p[4]);
                        FilterSpec fs = new FilterSpec(lat, lon, catSet, minStars, pc);
                        oos.writeObject(new Message(Message.MessageType.TASK, fs));
                        break;
                    }
                    case "buy": {
                        String[] p = parts[1].split(" ");
                        String store = p[0];
                        String[] items = p[1].split(",");
                        Map<String,Integer> map = new HashMap<>();
                        for (String it : items) {
                            String[] kv = it.split(":" );
                            map.put(kv[0], Integer.parseInt(kv[1]));
                        }
                        Sale sale = new Sale(store, map);
                        oos.writeObject(new Message(Message.MessageType.SALE, sale));
                        break;
                    }
                    case "rate": {
                        String[] p = parts[1].split(" ");
                        String store = p[0];
                        int stars = Integer.parseInt(p[1]);
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
