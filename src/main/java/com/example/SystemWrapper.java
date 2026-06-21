package com.example;

/**
 * System.exit() のテスト可能なラッパー。
 * このクラスを介することで Mockito の mockStatic でモックできる。
 */
public class SystemWrapper {

    public static void exit(int code) {
        System.exit(code);
    }
}
