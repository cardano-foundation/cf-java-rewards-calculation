package org.cardanofoundation.rewards.validation.data.provider;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.calculation.enums.MirPot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.EnabledIf;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Set;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ComponentScan
@EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
public class DbSyncDataProviderTest {

    @Autowired
    DbSyncDataProvider dbSyncDataProvider;

    @Value("${cardano.protocol.magic}")
    private int cardanoProtocolMagic;

    NetworkConfig networkConfig;

    @BeforeAll
    public void setup() {
        networkConfig = NetworkConfig.getNetworkConfigByNetworkMagic(cardanoProtocolMagic);
    }


    @Test
    public void testGetEpochs() {
        Epoch epoch = dbSyncDataProvider.getEpochInfo(220, networkConfig);
        Assertions.assertEquals(epoch.getNumber(), 220);
        Assertions.assertEquals(epoch.getFees(), new BigInteger("5135934788"));
        Assertions.assertEquals(epoch.getBlockCount(), 21627);
    }

    @Test
    public void testGetEpochInfoOf215() {
        Epoch epoch = dbSyncDataProvider.getEpochInfo(215, networkConfig);
        Assertions.assertEquals(epoch.getNumber(), 215);
        Assertions.assertEquals(epoch.getFees(), new BigInteger("8110049274"));
        Assertions.assertEquals(epoch.getBlockCount(), 21572);
        Assertions.assertEquals(epoch.getNonOBFTBlockCount(), 5710);
    }

    @Test
    public void testGetAdaPots() {
        AdaPots adaPots = dbSyncDataProvider.getAdaPotsForEpoch(220);
        Assertions.assertEquals(adaPots.getTreasury(), new BigInteger("94812346026398"));
        Assertions.assertEquals(adaPots.getReserves(), new BigInteger("13120582265809833"));
        Assertions.assertEquals(adaPots.getRewards(), new BigInteger("151012138061367"));
    }

    @Test
    public void testGetProtocolParameters() {
        ProtocolParameters protocolParameters = dbSyncDataProvider.getProtocolParametersForEpoch(220);
        Assertions.assertEquals(protocolParameters.getDecentralisation(), BigDecimal.valueOf(0.64));
        Assertions.assertEquals(protocolParameters.getTreasuryGrowRate(), BigDecimal.valueOf(0.2));
        Assertions.assertEquals(protocolParameters.getMonetaryExpandRate(), BigDecimal.valueOf(0.003));
        Assertions.assertEquals(protocolParameters.getPoolOwnerInfluence(), BigDecimal.valueOf(0.3));
        Assertions.assertEquals(protocolParameters.getOptimalPoolCount(), 150);
    }

    @Test
    public void testGetPoolHistory() {
        String poolId = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";
        int epoch = 220;
        PoolState poolState = dbSyncDataProvider.getPoolHistory(poolId, epoch);
        Assertions.assertEquals(poolState.getActiveStake(), new BigInteger("27523186299296"));
        Assertions.assertEquals(poolState.getBlockCount(), 10);
        Assertions.assertEquals(poolState.getFixedCost(), new BigInteger("340000000"));
        Assertions.assertEquals(poolState.getMargin(), new BigDecimal("0.009"));
        Assertions.assertEquals(poolState.getPoolFees(), new BigInteger("475116283"));
    }

    @Test
    public void testGetMirCertificatesInEpoch() {
        int epoch = 374;
        List<MirCertificate> mirCertificates = dbSyncDataProvider.getMirCertificatesInEpoch(epoch);

        BigInteger totalRewards = BigInteger.ZERO;
        for (MirCertificate mirCertificate : mirCertificates) {
            if (mirCertificate.getPot() == MirPot.TREASURY) {
                totalRewards = totalRewards.add(mirCertificate.getTotalRewards());
            }
        }

        Assertions.assertEquals(totalRewards, new BigInteger("39432064006444"));
    }

    @Test
    public void testGetRetiredPoolsInEpoch218() {
        int epoch = 218;
        Set<RetiredPool> retiredPools = dbSyncDataProvider.getRetiredPoolsInEpoch(epoch);

        Assertions.assertEquals(7, retiredPools.size());
    }
}
