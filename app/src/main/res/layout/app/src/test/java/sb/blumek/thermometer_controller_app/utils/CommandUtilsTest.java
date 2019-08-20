package sb.blumek.thermometer_controller_app.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandUtils test")
class CommandUtilsTest {

    @Nested
    @DisplayName("GetCommand test")
    class GetCommand {
        @Test
        @DisplayName("Get command when passed one valid command")
        void getCommandTest_OneValidCommand() {
            String command = "[T1-29.00]";
            assertEquals(command, CommandUtils.getCommand(command),
                    "Should return expected command");
        }

        @Test
        @DisplayName("Get command when passed null command")
        void getCommandTest_NullCommand() {
            assertNull(CommandUtils.getCommand(null),
                    "Should return null");
        }

        @Test
        @DisplayName("Get command when passed two valid commands")
        void getCommandTest_TwoValidCommands() {
            String command = "[T1-29.00][T2-19.00]";
            assertEquals(command, CommandUtils.getCommand(command),
                    "Should return expected command");
        }

        @Test
        @DisplayName("Get command when passed one invalid command")
        void getCommandTest_OneInvalidCommand() {
            String command = "[T1-29.0";
            assertNull(CommandUtils.getCommand(command),
                    "Should return null");
        }

        @Test
        @DisplayName("Get command when passed two valid commands")
        void getCommandTest_OneValidCommandAndInvalid() {
            String command = "[T1-29.00][T2-1";
            assertEquals("[T1-29.00]", CommandUtils.getCommand(command),
                    "Should return expected command");
        }
    }
}