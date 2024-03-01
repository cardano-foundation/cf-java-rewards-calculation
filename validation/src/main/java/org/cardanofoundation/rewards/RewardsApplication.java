package org.cardanofoundation.rewards;

import org.cardanofoundation.rewards.validation.data.fetcher.DbSyncDataFetcher;
import org.cardanofoundation.rewards.validation.data.fetcher.KoiosDataFetcher;
import org.cardanofoundation.rewards.validation.data.plotter.JsonDataPlotter;
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

@EnableConfigurationProperties
@EntityScan({"org.cardanofoundation.rewards.*", "org.cardanofoundation.*"})
@SpringBootApplication
public class RewardsApplication implements ApplicationRunner {
  private static final Logger logger = LoggerFactory.getLogger(RewardsApplication.class);

  @Value("${application.run.mode}")
  private String runMode;

  @Value("${application.fetch.override}")
  private boolean overrideFetchedData;

  @Value("${spring.profiles.active:Unknown}")
  private String activeProfiles;

  @Value("${json.data-provider.source}")
  private String sourceFolder;

  @Autowired
  private ApplicationContext context;

  @Autowired
  private JsonDataPlotter jsonDataPlotter;

  @Autowired
  private KoiosDataFetcher koiosDataFetcher;

  @Autowired(required = false)
  private DbSyncDataFetcher dbSyncDataFetcher;

  public static void main(String[] args) {
    SpringApplication.run(RewardsApplication.class, args);
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {

      if (runMode == null) {
        logger.warn("No run mode specified. Set the environment variable RUN_MODE in your .env file to 'fetch' or 'plot' or 'test'");
        int exitCode = SpringApplication.exit(context, (ExitCodeGenerator) () -> 0);
        System.exit(exitCode);
      }

      int startEpoch = 208;
      int endEpoch = 464;

      if (runMode.equals("fetch")) {
          boolean override = overrideFetchedData;
          logger.info("Override fetched data: " + override);

          if (activeProfiles.contains("db-sync")) {
            logger.info("DB Sync data provider is active. Fetching data from DB Sync...");

            for (int epoch = startEpoch; epoch < endEpoch; epoch++) {
              logger.info("Fetching data for epoch with the DB sync data provider " + epoch);
              dbSyncDataFetcher.fetch(epoch, override);
            }
          }

          if (activeProfiles.contains("koios")) {
            logger.info("Koios data provider is active. Fetching data from Koios...");
            for (int epoch = startEpoch; epoch < endEpoch; epoch++) {
                logger.info("Fetching data for epoch with the Koios data provider " + epoch);
                koiosDataFetcher.fetch(epoch, override);
            }
          }
      } else if (runMode.equals("plot")) {
        jsonDataPlotter.plot(startEpoch, endEpoch);
      } else if (!runMode.equals("test")) {
          logger.warn("Unknown run mode: " + runMode);
      }

      if (!runMode.equals("test")) {
          logger.info("Done. Exiting...");
          int exitCode = SpringApplication.exit(context, () -> 0);
          System.exit(exitCode);
      }
  }
}
