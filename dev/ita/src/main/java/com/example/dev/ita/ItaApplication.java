package com.example.dev.ita;

import com.example.dev.common.AppConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = {
        "com.example.app.jsp",
        "com.example.dev.ita"
    }
)
public class ItaApplication {

    public static void main(String[] args) {
        System.out.println("Starting " + AppConstants.APP_NAME);
        SpringApplication.run(ItaApplication.class, args);
    }
}
