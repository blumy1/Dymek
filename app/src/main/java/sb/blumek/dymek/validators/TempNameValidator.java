package sb.blumek.dymek.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TempNameValidator implements Validator {
    private static final String VALID_NAME_CONTAINS = "[A-Za-z0-9:\\- _<>\"\'.,(){}]+";

    private String tempName;
    private Pattern namePattern;
    private Matcher patternMatcher;

    public TempNameValidator(String tempName) {
        this.tempName = tempName;
        compilePattern();
        getPatternMatcher(namePattern);
    }

    @Override
    public boolean isValid() {
        return !tempName.isEmpty() && patternMatcher.matches();
    }

    private void getPatternMatcher(Pattern pattern) {
        patternMatcher = pattern.matcher(tempName);
    }

    private void compilePattern() {
        namePattern = Pattern.compile(VALID_NAME_CONTAINS);
    }
}
