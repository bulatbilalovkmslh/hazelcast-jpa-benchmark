package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "org.example",
})
public class Node1Application {
    public static void main(String[] args) {
        SpringApplication.run(Node1Application.class, args);
    }
}