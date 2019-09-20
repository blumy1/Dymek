package sb.blumek.dymek.validators;

public class TempRangeValidator implements Validator {
    private static final double MIN_TEMP_VALUE = -127;
    private static final double MAX_TEMP_VALUE = 127;

    private Double tempValue;

    public TempRangeValidator(Double tempValue) {
        this.tempValue = tempValue;
    }

    @Override
    public boolean isValid() {
        return tempValue != null && tempValue >= MIN_TEMP_VALUE && tempValue <= MAX_TEMP_VALUE;
    }
}
