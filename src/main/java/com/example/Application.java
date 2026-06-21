package com.example;

public class Application {

    public void run(String[] args) {
        if (args == null || args.length == 0) {
            System.err.println("Error: no arguments provided");
            SystemWrapper.exit(1);
            return;
        }

        String command = args[0];
        switch (command) {
            case "hello" -> System.out.println("Hello, World!");
            case "exit" -> {
                System.out.println("Exiting with code 0");
                SystemWrapper.exit(0);
            }
            default -> {
                System.err.println("Unknown command: " + command);
                SystemWrapper.exit(2);
            }
        }
    }

    public static void main(String[] args) {
        new Application().run(args);
    }
}
