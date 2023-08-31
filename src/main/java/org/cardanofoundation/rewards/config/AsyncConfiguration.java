package org.cardanofoundation.rewards.config;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration implements AsyncConfigurer {
  @Value("${spring.task.execution.pool.core-size}")
  private int core;

  @Value("${spring.task.execution.pool.max-size}")
  private int max;

  @Value("${spring.task.execution.thread-name-prefix}")
  private String name;

  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(core);
    executor.setMaxPoolSize(max);
    executor.setThreadNamePrefix(name);
    executor.initialize();
    return executor;
  }
}
