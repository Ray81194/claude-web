package com.example;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Application System.exit Tests")
class ApplicationTest {

    private Application app;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        app = new Application();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // SystemWrapper.exit() をキャッチするための例外クラス
    static class ExitException extends RuntimeException {
        final int status;
        ExitException(int status) {
            super("System.exit(" + status + ") が呼ばれました");
            this.status = status;
        }
    }

    // SystemWrapper.exit をモックし ExitException をスローするヘルパー
    private MockedStatic<SystemWrapper> mockSystemExit() {
        MockedStatic<SystemWrapper> sysMock = mockStatic(SystemWrapper.class);
        sysMock.when(() -> SystemWrapper.exit(anyInt()))
               .thenAnswer(inv -> { throw new ExitException(inv.getArgument(0)); });
        return sysMock;
    }

    @Test
    @DisplayName("引数なしの場合は exit code 1 で終了する")
    void exitCode1WhenNoArguments() {
        try (MockedStatic<SystemWrapper> ignored = mockSystemExit()) {
            ExitException ex = assertThrows(ExitException.class, () -> app.run(new String[]{}));
            assertEquals(1, ex.status);
        }
    }

    @Test
    @DisplayName("null引数の場合は exit code 1 で終了する")
    void exitCode1WhenNullArguments() {
        try (MockedStatic<SystemWrapper> ignored = mockSystemExit()) {
            ExitException ex = assertThrows(ExitException.class, () -> app.run(null));
            assertEquals(1, ex.status);
        }
    }

    @Test
    @DisplayName("'exit'コマンドの場合は exit code 0 で終了する")
    void exitCode0WhenExitCommand() {
        try (MockedStatic<SystemWrapper> ignored = mockSystemExit()) {
            ExitException ex = assertThrows(ExitException.class, () -> app.run(new String[]{"exit"}));
            assertEquals(0, ex.status);
        }
    }

    @Test
    @DisplayName("不明なコマンドの場合は exit code 2 で終了する")
    void exitCode2WhenUnknownCommand() {
        try (MockedStatic<SystemWrapper> ignored = mockSystemExit()) {
            ExitException ex = assertThrows(ExitException.class, () -> app.run(new String[]{"unknown"}));
            assertEquals(2, ex.status);
        }
    }

    @Test
    @DisplayName("'hello'コマンドは System.exit を呼ばない")
    void helloCommandDoesNotCallSystemExit() {
        try (MockedStatic<SystemWrapper> sysMock = mockStatic(SystemWrapper.class)) {
            app.run(new String[]{"hello"});
            sysMock.verifyNoInteractions();
        }
    }

    @Test
    @DisplayName("引数なしの場合は標準エラーにメッセージが出力される")
    void stderrMessageWhenNoArguments() {
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(errContent));

        try (MockedStatic<SystemWrapper> ignored = mockSystemExit()) {
            assertThrows(ExitException.class, () -> app.run(new String[]{}));
        }

        assertTrue(errContent.toString().contains("no arguments provided"));
    }

    @Test
    @DisplayName("'exit'コマンドは標準出力にメッセージが出力される")
    void stdoutMessageWhenExitCommand() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        try (MockedStatic<SystemWrapper> ignored = mockSystemExit()) {
            assertThrows(ExitException.class, () -> app.run(new String[]{"exit"}));
        }

        assertTrue(outContent.toString().contains("Exiting with code 0"));
    }
}
