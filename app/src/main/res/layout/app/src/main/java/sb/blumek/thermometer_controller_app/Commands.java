package sb.blumek.thermometer_controller_app;

public class Commands {
    public static final String IS_COMMAND = "\\[.*\\]";

    public static final String OFF_ALARM = "[SW11]";
//    public static final String ALARM_UP = "[SW1-1]";
//    public static final String ALARM_DOWN = "[SW1-0]";
    public static final String APP_HI = "[DymekAppHi]";
    public static final String SET_TEMP_1_MIN = "[TMIN1-%.2f]";
    public static final String SET_TEMP_1_MAX = "[TMAX1-%.2f]";
    public static final String SET_TEMP_2_MIN = "[TMIN2-%.2f]";
    public static final String SET_TEMP_2_MAX = "[TMAX2-%.2f]";
    public static final String SET_TEMP_1_NAME = "[TN1-%s]";
    public static final String SET_TEMP_2_NAME = "[TN2-%s]";

    public static final String DEVICE_HI = "\\[DymekHi\\]";
    public static final String DEVICE_HI_CLR = "[DymekHi]";
    public static final String ALARM_UP = "\\[BUZ-1\\]";
    public static final String ALARM_DOWN = "\\[BUZ-0\\]";
    public static final String TEMP_1_VALUE = "\\[T1-(-?\\d+(\\.\\d+)?)\\]";
    public static final String TEMP_2_VALUE = "\\[T2-(-?\\d+(\\.\\d+)?)\\]";
    public static final String TEMP_1_MIN_VALUE = "\\[T1Min-(-?\\d+(\\.\\d+)?)\\]";
    public static final String TEMP_1_MAX_VALUE = "\\[T1Max-(-?\\d+(\\.\\d+)?)\\]";
    public static final String TEMP_2_MIN_VALUE = "\\[T2Min-(-?\\d+(\\.\\d+)?)\\]";
    public static final String TEMP_2_MAX_VALUE = "\\[T2Max-(-?\\d+(\\.\\d+)?)\\]";
    public static final String TEMP_1_NAME = "\\[TNam1-([A-Za-z0-9:\\- <>\"'\\.,\\[\\]\\(\\)\\{\\}]+)\\/]";
    public static final String TEMP_2_NAME = "\\[TNam2-([A-Za-z0-9:\\- <>\"'\\.,\\[\\]\\(\\)\\{\\}]+)\\/]";

    public static final String VALID_NAME = "[A-Za-z0-9:\\- <>\"\'\\.,\\[\\]\\(\\)\\{\\}/]+";
}
