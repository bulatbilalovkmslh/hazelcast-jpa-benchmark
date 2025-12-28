package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "org.example"
})
public class Node2Application {
    public static void main(String[] args) {
        SpringApplication.run(Node2Application.class, args);
    }
}
