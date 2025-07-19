package com.iceekb.dushnila.message.util;

import com.iceekb.dushnila.TestUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class TextUtilTest extends TestUtils {

    @ParameterizedTest
    @CsvSource({
            "/command \"from\" \"to\", false, COMMAND, from, to",
            "/command \"from\" \"to\" \"to\", true, COMMAND, from, to",
            "/command \"from\" \"to and to\", false, COMMAND, from, to and to",
            "/command \"from and from\" \"to\" \"to\", true, COMMAND, from and from, to",
            "/WORD \"from and from\" \"to\", true, WORD, from and from, to"
    })
    void line2paramTest(String command, Boolean isError, String commandTxt, String from, String to) {

        Map<String, String> data = TextUtil.line2param(command);

        if (isError) {
            assertEquals(1, data.size());
            if (commandTxt.equals("WORD")) {
                assertEquals("Преобразуем только слово, не фразу...", data.get("error"));
            } else {
                assertEquals(FORMAT_ERROR, data.get("error"));
            }

        } else {
            assertEquals(3, data.size());
            assertEquals(commandTxt, data.get("command"));
            assertEquals(from, data.get("from"));
            assertEquals(to, data.get("to"));
        }

    }

    @ParameterizedTest
    @CsvSource({
            "/command \"cmd\", false, COMMAND, cmd",
            "/command cmd, true, COMMAND, cmd"
    })
    void line1paramTest(String command, Boolean isError, String commandTxt, String cmd) {
        Map<String, String> data = TextUtil.line1param(command);

        if (isError) {
            assertEquals(1, data.size());
            assertEquals(FORMAT_ERROR, data.get("error"));
        } else {
            assertEquals(2, data.size());
            assertEquals(commandTxt, data.get("command"));
            assertEquals(cmd, data.get("param"));
        }
    }
}
