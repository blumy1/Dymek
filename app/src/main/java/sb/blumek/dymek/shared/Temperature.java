package sb.blumek.dymek.shared;

public class Temperature {
    private String name = "Temp";
    private Double tempMin = 0.0;
    private Double temp = 0.0;
    private Double tempMax = 100.0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getTempMin() {
        return tempMin;
    }

    public void setTempMin(Double tempMin) {
        this.tempMin = tempMin;
    }

    public Double getTemp() {
        return temp;
    }

    public void setTemp(Double temp) {
        this.temp = temp;
    }

    public Double getTempMax() {
        return tempMax;
    }

    public void setTempMax(Double tempMax) {
        this.tempMax = tempMax;
    }

    @Override
    public String toString() {
        return "Temperature [" +
                "name='" + name + '\'' +
                ", tempMin=" + tempMin +
                ", temp=" + temp +
                ", tempMax=" + tempMax +
                ']';
    }
}
