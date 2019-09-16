package sb.blumek.dymek.listeners;

public interface BluetoothListener {
    void onConnect();
    void onConnectError(Exception e);
    void onRead(byte[] data);
    void onIoError(Exception e);
}
