package sb.blumek.thermometer_controller_app.models;

public class Device {
    private String address;
    private String name;

    public Device() {
    }

    public Device(String address, String name) {
        this.address = address;
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
