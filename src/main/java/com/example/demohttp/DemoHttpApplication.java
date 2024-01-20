
package com.example.demohttp;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static java.time.temporal.ChronoUnit.SECONDS;

@SpringBootApplication
public class DemoHttpApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoHttpApplication.class, args);
    }

    @Bean
    public ThreadPoolTaskExecutor customTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor() {
            @Override
            public void shutdown() {
                super.shutdown();
                System.out.println("customShutdown");
            }
        };
        executor.setMaxPoolSize(1);
        executor.setThreadNamePrefix("customThread-");
        return executor;

    }

    @RestController
    @Slf4j
    public static class DemoController {

        private final ThreadPoolTaskExecutor customTaskExecutor;
        private final HttpClient httpClietn;
        private final HttpRequest request;

        public DemoController(ThreadPoolTaskExecutor customTaskExecutor) throws URISyntaxException {
            this.customTaskExecutor = customTaskExecutor;
            this.request = HttpRequest.newBuilder()
                    .uri(new URI("https://official-joke-api.appspot.com/random_joke"))
                    .timeout(Duration.of(10, SECONDS))
                    .version(HttpClient.Version.HTTP_1_1)
                    .GET()
                    .build();
            this.httpClietn = HttpClient.newBuilder().executor(customTaskExecutor)
                    .connectTimeout(Duration.of(10, SECONDS))
                    .build();

        }

        @GetMapping("testHttpAsync")
        public CompletableFuture<ArrayList<String>> getAsync(@RequestParam(name = "n") int n) {
            return combineHttpRequests(n);
        }

        @GetMapping("testHttpAsyncBatch")
        public CompletableFuture<ArrayList<String>> getAsyncBatch(
                @RequestParam(name = "batchSize") int batchSize,
                @RequestParam(name = "n") int n) {
            CompletableFuture<ArrayList<String>> result = CompletableFuture.completedFuture(new ArrayList<>());
            for (int i = 0; i < batchSize; i++) {
                result = result.thenCompose(
                        accumulator -> combineHttpRequests(n)
                                .thenApply(batchResult -> mergeResult(accumulator, batchResult))
                );
            }
            return result;
        }

        private static ArrayList<String> mergeResult(ArrayList<String> acc, ArrayList<String> batchResult) {
            log.warn("Execute batch in - " + Thread.currentThread().getName());
            acc.addAll(batchResult);
            return acc;
        }

        private CompletableFuture<ArrayList<String>> combineHttpRequests(int n) {
            CompletableFuture<ArrayList<String>> baseFuture = CompletableFuture.completedFuture(new ArrayList<>(n));
            for (int i = 0; i < n; i++) {
                var httpResponseFuture = getHttpResponseCompletableFuture();
                baseFuture = baseFuture.thenCombine(httpResponseFuture, (acc, response) -> {
                    acc.add(response);
                    return acc;
                });
            }
            return baseFuture;
        }

        private CompletableFuture<String> getHttpResponseCompletableFuture() {
            return httpClietn.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApplyAsync(HttpResponse::body, customTaskExecutor);
        }
    }
}
