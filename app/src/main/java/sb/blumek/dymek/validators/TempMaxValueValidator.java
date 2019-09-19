package sb.blumek.dymek.validators;

public class TempMaxValueValidator implements Validator {
    private static final double MAX_TEMP_VALUE = 127;

    private double maxTempValue;

    public TempMaxValueValidator(double maxTempValue) {
        this.maxTempValue = maxTempValue;
    }

    @Override
    public boolean isValid() {
        return maxTempValue <= MAX_TEMP_VALUE;
    }
}
