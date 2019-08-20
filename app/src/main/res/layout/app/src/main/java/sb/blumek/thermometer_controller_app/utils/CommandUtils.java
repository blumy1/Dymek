package sb.blumek.thermometer_controller_app.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sb.blumek.thermometer_controller_app.Commands;

public class CommandUtils {
    public static boolean isCommand(String exp) {
        Pattern pattern = Pattern.compile(Commands.IS_COMMAND);
        Matcher matcher = pattern.matcher(exp);
        if (matcher.find()) {
            return true;
        }
        return false;
    }

    public static boolean isValidName(String exp) {
        Pattern pattern = Pattern.compile(Commands.VALID_NAME);
        Matcher matcher = pattern.matcher(exp);
        if (matcher.matches()) {
            return true;
        }
        return false;
    }

    public static String getCommand(String exp) {
        if (exp == null)
            return null;

        Pattern pattern = Pattern.compile(Commands.IS_COMMAND);
        Matcher matcher = pattern.matcher(exp);

        if (matcher.find()) {
            return matcher.group(0);
        }
        return null;
    }

    public static String removeCommandFromExp(String exp) {
        Pattern pattern = Pattern.compile(Commands.IS_COMMAND);
        Matcher matcher = pattern.matcher(exp);
        if (matcher.find()) {
            return matcher.replaceAll("");
        }
        return null;
    }

    public static String getStringFromExp(String exp, String regExPattern, int group) {
        Pattern pattern = Pattern.compile(regExPattern);
        Matcher matcher = pattern.matcher(exp);
        if (matcher.find()) {
            return matcher.group(group);
        }
        return null;
    }

    public static Double getDoubleFromExp(String exp, String regExPattern, int group) {
        Pattern pattern = Pattern.compile(regExPattern);
        Matcher matcher = pattern.matcher(exp);
        if (matcher.find()) {
            String found = matcher.group(1);
            return Double.valueOf(found);
        }
        return null;
    }
}
