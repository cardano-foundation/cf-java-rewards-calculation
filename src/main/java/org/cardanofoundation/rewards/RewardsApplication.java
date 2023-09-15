package org.cardanofoundation.rewards;

import org.cardanofoundation.rewards.data.fetcher.DataFetcher;
import org.cardanofoundation.rewards.data.fetcher.KoiosDataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

import java.util.List;

@EnableConfigurationProperties
@EntityScan({"org.cardanofoundation.rewards.*", "org.cardanofoundation.*"})
@SpringBootApplication
public class RewardsApplication implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(RewardsApplication.class);

  @Autowired
  private ApplicationContext context;

  public static void main(String[] args) {
    SpringApplication.run(RewardsApplication.class, args);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    for (String name : args.getOptionNames()) {
      if (name.equals("action")) {
        List<String> action = args.getOptionValues("action");
        if (action == null) {
          logger.warn("No action specified. Example usage: --action=fetch --override");
          return;
        }

        if (action.get(0).equals("fetch")) {
          boolean override = args.containsOption("override");
          KoiosDataFetcher dataFetcher = new KoiosDataFetcher();
          for (int epoch = 208; epoch < 433; epoch++) {
            logger.info("Fetching data for epoch " + epoch);
            dataFetcher.fetch(epoch, override);
          }
        } else {
          logger.warn("Unknown action: " + action.get(0));
        }
      } else {
        logger.warn("Unknown option: " + name);
      }
    }

    if (args.getOptionNames().size() == 0) {
      logger.warn("No options specified. Example usage: --action=fetch");
    }

    int exitCode = SpringApplication.exit(context, (ExitCodeGenerator) () -> 0);
    System.exit(exitCode);
  }
}
