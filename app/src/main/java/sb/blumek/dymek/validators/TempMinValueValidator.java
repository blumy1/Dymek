package sb.blumek.dymek.validators;

public class TempMinValueValidator implements Validator {
    private static final double MIN_TEMP_VALUE = -127;

    private double minTempValue;

    public TempMinValueValidator(double minTempValue) {
        this.minTempValue = minTempValue;
    }

    @Override
    public boolean isValid() {
        return minTempValue >= MIN_TEMP_VALUE;
    }
}
