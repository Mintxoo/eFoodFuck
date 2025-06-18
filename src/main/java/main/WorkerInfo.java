package main;

public class WorkerInfo {
    private final String id;
    private final String host;
    private final int port;

    public WorkerInfo(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }

    @Override
    public String toString() {
        return "WorkerInfo{" +
               "id='" + id + '\'' +
               ", host='" + host + '\'' +
               ", port=" + port +
               '}';
    }
}
