package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Application を別 JVM プロセスとして起動し、実際の System.exit() 呼び出しを検証するテスト。
 * Java 21 では SecurityManager が廃止されたため、サブプロセス方式を採用。
 */
@DisplayName("Application System.exit Tests")
class ApplicationTest {

    private static final String JAVA_CMD = ProcessHandle.current().info().command()
            .orElseThrow(() -> new IllegalStateException("Cannot determine java executable path"));
    private static final String CLASSPATH = System.getProperty("java.class.path");

    record ProcessResult(int exitCode, String stdout, String stderr) {}

    /** Application を子プロセスとして実行し、stdout/stderr と終了コードを返す */
    private ProcessResult run(String... appArgs) throws Exception {
        List<String> cmd = new ArrayList<>(List.of(JAVA_CMD, "-cp", CLASSPATH, "com.example.Application"));
        Collections.addAll(cmd, appArgs);

        Process p = new ProcessBuilder(cmd).start();

        // stdout と stderr を並行して読み取る（パイプバッファ枯渇によるデッドロック防止）
        CompletableFuture<byte[]> stdoutFuture = CompletableFuture.supplyAsync(() -> readBytes(p.getInputStream()));
        CompletableFuture<byte[]> stderrFuture = CompletableFuture.supplyAsync(() -> readBytes(p.getErrorStream()));

        p.waitFor(10, TimeUnit.SECONDS);

        return new ProcessResult(
                p.exitValue(),
                new String(stdoutFuture.get(10, TimeUnit.SECONDS)),
                new String(stderrFuture.get(10, TimeUnit.SECONDS))
        );
    }

    private static byte[] readBytes(InputStream is) {
        try { return is.readAllBytes(); }
        catch (IOException e) { return new byte[0]; }
    }

    @Test
    @DisplayName("引数なしの場合は exit code 1 で終了する")
    void exitCode1WhenNoArguments() throws Exception {
        ProcessResult result = run();
        assertEquals(1, result.exitCode());
    }

    @Test
    @DisplayName("'exit'コマンドの場合は exit code 0 で終了する")
    void exitCode0WhenExitCommand() throws Exception {
        ProcessResult result = run("exit");
        assertEquals(0, result.exitCode());
    }

    @Test
    @DisplayName("不明なコマンドの場合は exit code 2 で終了する")
    void exitCode2WhenUnknownCommand() throws Exception {
        ProcessResult result = run("unknown");
        assertEquals(2, result.exitCode());
    }

    @Test
    @DisplayName("'hello'コマンドは exit code 0 以外で終了しない")
    void helloCommandExitsNormally() throws Exception {
        ProcessResult result = run("hello");
        assertEquals(0, result.exitCode());
    }

    @Test
    @DisplayName("引数なしの場合は標準エラーにメッセージが出力される")
    void stderrMessageWhenNoArguments() throws Exception {
        ProcessResult result = run();
        assertTrue(result.stderr().contains("no arguments provided"));
    }

    @Test
    @DisplayName("不明なコマンドの場合は標準エラーにコマンド名が出力される")
    void stderrContainsCommandNameWhenUnknown() throws Exception {
        ProcessResult result = run("badcmd");
        assertTrue(result.stderr().contains("badcmd"));
    }

    @Test
    @DisplayName("'hello'コマンドは標準出力に Hello, World! を表示する")
    void helloCommandPrintsToStdout() throws Exception {
        ProcessResult result = run("hello");
        assertTrue(result.stdout().contains("Hello, World!"));
    }

    @Test
    @DisplayName("'exit'コマンドは標準出力に終了メッセージを表示する")
    void exitCommandPrintsToStdout() throws Exception {
        ProcessResult result = run("exit");
        assertTrue(result.stdout().contains("Exiting with code 0"));
    }
}
