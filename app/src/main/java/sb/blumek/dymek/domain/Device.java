package sb.blumek.dymek.domain;

public class Device {
    private String name;
    private String address;

    public Device(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "Device [" +
                "name='" + name + '\'' +
                ", address='" + address + '\'' +
                ']';
    }
}
