package org.cardanofoundation.rewards;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties
@EntityScan({"org.cardanofoundation.rewards.*", "org.cardanofoundation.*"})
@SpringBootApplication
public class RewardsApplication {

  public static void main(String[] args) {
    SpringApplication.run(RewardsApplication.class, args);
  }
}
