package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ComponentScan
@ActiveProfiles("db-sync")
public class DbSyncDataProviderTest {

    @Autowired
    DbSyncDataProvider dbSyncDataProvider;

    @Test
    public void testGetEpochs() {
        Epoch epoch = dbSyncDataProvider.getEpochInfo(220);
        Assertions.assertEquals(epoch.getNumber(), 220);
        Assertions.assertEquals(epoch.getFees(), 5135934788.0);
        Assertions.assertEquals(epoch.getBlockCount(), 21627);
    }

    @Test
    public void testGetAdaPots() {
        AdaPots adaPots = dbSyncDataProvider.getAdaPotsForEpoch(220);
        Assertions.assertEquals(adaPots.getTreasury(), 9.4812346026398E13);
        Assertions.assertEquals(adaPots.getReserves(), 1.3120582265809832E16);
        Assertions.assertEquals(adaPots.getRewards(), 1.51012138061367E14);
    }

    @Test
    public void testGetProtocolParameters() {
        ProtocolParameters protocolParameters = dbSyncDataProvider.getProtocolParametersForEpoch(220);
        Assertions.assertEquals(protocolParameters.getDecentralisation(), 0.64);
        Assertions.assertEquals(protocolParameters.getTreasuryGrowRate(), 0.2);
        Assertions.assertEquals(protocolParameters.getMonetaryExpandRate(), 0.003);
        Assertions.assertEquals(protocolParameters.getPoolOwnerInfluence(), 0.3);
        Assertions.assertEquals(protocolParameters.getOptimalPoolCount(), 150);
    }

    @Test
    public void testGetPoolHistory() {
        String poolId = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";
        int epoch = 220;
        PoolHistory poolHistory = dbSyncDataProvider.getPoolHistory(poolId, epoch);
        Assertions.assertEquals(poolHistory.getActiveStake(), 27523186299296.0);
        Assertions.assertEquals(poolHistory.getBlockCount(), 10);
        Assertions.assertEquals(poolHistory.getFixedCost(), 340000000.0);
        Assertions.assertEquals(poolHistory.getMargin(), 0.009);
        Assertions.assertEquals(poolHistory.getDelegatorRewards(), 14877804008.0);
        Assertions.assertEquals(poolHistory.getPoolFees(), 475116283.0);
    }

    @Test
    public void testGetHistoryOfPoolOwnersInEpoch() {
        String poolId = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";
        int epoch = 220;
        PoolOwnerHistory poolOwnerHistory = dbSyncDataProvider.getHistoryOfPoolOwnersInEpoch(poolId, epoch);
        Assertions.assertEquals(poolOwnerHistory.getActiveStake(), 4.76793511093E11);
    }
}
