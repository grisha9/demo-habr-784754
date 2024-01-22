package com.example.demo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public ExecutorService customTaskExecutor() {
        return new ThreadPoolExecutor(1, 1, 60L, TimeUnit.SECONDS, new SynchronousQueue<>()) {
            @Override
            public void shutdown() {
                super.shutdown();
                System.out.println("customShutdown");
            }
        };
    }

    @RestController
    @Slf4j
    public static class DemoController {

        @Autowired
        private ExecutorService customTaskExecutor;

        @GetMapping("test")
        @SneakyThrows
        public String get() {
            Future<String> future = customTaskExecutor.submit(() -> {
                log.warn("Execute start method - " + Thread.currentThread().getName());
                Thread.sleep(5_000);
                log.warn("Execute end method - " + Thread.currentThread().getName());
                return "test";
            });
            return future.get();
        }
    }
}
