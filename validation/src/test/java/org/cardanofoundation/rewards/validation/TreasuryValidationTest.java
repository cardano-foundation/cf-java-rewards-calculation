package org.cardanofoundation.rewards.validation;

import java.math.BigInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.validation.data.provider.DataProvider;
import org.cardanofoundation.rewards.validation.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.data.provider.KoiosDataProvider;
import org.cardanofoundation.rewards.validation.domain.TreasuryValidationResult;
import org.cardanofoundation.rewards.validation.enums.DataProviderType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.EnabledIf;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScan
@Slf4j
public class TreasuryValidationTest {

  @Autowired
  KoiosDataProvider koiosDataProvider;

  @Autowired
  JsonDataProvider jsonDataProvider;

  @Autowired(required = false)
  DbSyncDataProvider dbSyncDataProvider;

  @Value("${cardano.protocol.magic}")
  private int cardanoProtocolMagic;

  NetworkConfig networkConfig;

  @BeforeAll
  public void setup() {
    networkConfig = NetworkConfig.getNetworkConfigByNetworkMagic(cardanoProtocolMagic);
  }

  void Test_calculateTreasury(final int epoch, DataProviderType dataProviderType) {

    DataProvider dataProvider;
    if (dataProviderType == DataProviderType.KOIOS) {
      dataProvider = koiosDataProvider;
    } else if (dataProviderType == DataProviderType.JSON) {
      dataProvider = jsonDataProvider;
    } else if (dataProviderType == DataProviderType.DB_SYNC) {
      dataProvider = dbSyncDataProvider;
    } else {
      throw new RuntimeException("Unknown data provider type: " + dataProviderType);
    }

    BigInteger unspendableEarnedRewards = BigInteger.ZERO;
    if (epoch == 215) {
      unspendableEarnedRewards = new BigInteger("53681623");
    } else if (epoch > 215) {
      log.warn("Unspendable rewards are not provided for epoch " + epoch + " and cannot be calculated within the TreasuryCalculation" +
              " as it needs other variables processed in the EpochCalculation, using zero value, but the test may fail.");
    }

    TreasuryValidationResult treasuryValidationResult = TreasuryValidation.calculateTreasuryForEpoch(epoch, dataProvider, networkConfig, unspendableEarnedRewards);
    BigInteger difference = treasuryValidationResult.getActualTreasury().subtract(treasuryValidationResult.getCalculatedTreasury());
    Assertions.assertEquals(BigInteger.ZERO, difference, "The difference " + difference.longValue() + " Lovelace between expected treasury value and actual treasury value is not zero");
  }

  static Stream<Integer> dataProviderRange() {
    return IntStream.range(210, 216).boxed();
  }

  @ParameterizedTest
  @MethodSource("dataProviderRange")
  void Test_calculateTreasuryWithJsonDataProvider(int epoch) {
    Test_calculateTreasury(epoch, DataProviderType.JSON);
  }

  @ParameterizedTest
  @MethodSource("dataProviderRange")
  @EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
  void Test_calculateTreasuryWithDbSyncDataProvider(int epoch) {
    Test_calculateTreasury(epoch, DataProviderType.DB_SYNC);
  }
}
