package com.farmatodo.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
@EnableConfigurationProperties(ProductSearchProperties.class)
public class AsyncConfig implements AsyncConfigurer {

	public static final String SEARCH_LOG_EXECUTOR = "searchLogExecutor";
	public static final String TRANSACTION_LOG_EXECUTOR = "transactionLogExecutor";

	private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

	private final ThreadPoolTaskExecutor searchLogExecutor = buildSearchLogExecutor();
	private final ThreadPoolTaskExecutor transactionLogExecutor = buildTransactionLogExecutor();

	@Bean(name = SEARCH_LOG_EXECUTOR)
	public ThreadPoolTaskExecutor searchLogExecutor() {
		return searchLogExecutor;
	}

	@Bean(name = TRANSACTION_LOG_EXECUTOR)
	public ThreadPoolTaskExecutor transactionLogExecutor() {
		return transactionLogExecutor;
	}

	@Override
	public Executor getAsyncExecutor() {
		return searchLogExecutor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (throwable, method, params) -> log.warn("Async task failed in {}", method.getName(), throwable);
	}

	private static ThreadPoolTaskExecutor buildSearchLogExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(4);
		executor.setQueueCapacity(200);
		executor.setThreadNamePrefix("search-log-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
		executor.initialize();
		return executor;
	}

	private static ThreadPoolTaskExecutor buildTransactionLogExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		executor.setMaxPoolSize(8);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("transaction-log-");
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		executor.initialize();
		return executor;
	}
}
