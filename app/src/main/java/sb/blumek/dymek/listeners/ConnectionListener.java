package sb.blumek.dymek.listeners;

public interface ConnectionListener {
    void onConnect();
    void onConnecting();
    void onDisconnect();
}
