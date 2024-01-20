package com.example.demo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
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

		@Autowired
		private ThreadPoolTaskExecutor customTaskExecutor;

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
