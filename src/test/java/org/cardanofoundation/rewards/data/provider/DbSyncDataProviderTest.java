package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.calculation.PoolRewardCalculation;
import org.cardanofoundation.rewards.calculation.TreasuryCalculation;
import org.cardanofoundation.rewards.entity.*;
import org.cardanofoundation.rewards.enums.MirPot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.cardanofoundation.rewards.util.CurrencyConverter.adaToLovelace;
import static org.cardanofoundation.rewards.util.CurrencyConverter.lovelaceToAda;

@SpringBootTest
@ComponentScan
@EnabledIf(expression = "#{environment.acceptsProfiles('db-sync')}", loadContext = true, reason = "DB Sync data provider must be available for this test")
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
    public void testGetEpochInfoOf215() {
        Epoch epoch = dbSyncDataProvider.getEpochInfo(215);
        Assertions.assertEquals(epoch.getNumber(), 215);
        Assertions.assertEquals(epoch.getFees(), 8.110049274E9);
        Assertions.assertEquals(epoch.getBlockCount(), 21572);
        Assertions.assertEquals(epoch.getNonOBFTBlockCount(), 5710);
        Assertions.assertEquals(epoch.getUnixTimeFirstBlock(), 1599083091);
        Assertions.assertEquals(epoch.getUnixTimeLastBlock(), 1599515063);
        Assertions.assertEquals(epoch.getOBFTBlockCount(), 15862);
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

    @Test
    public void testGetMirCertificatesInEpoch() {
        int epoch = 374;
        List<MirCertificate> mirCertificates = dbSyncDataProvider.getMirCertificatesInEpoch(epoch);

        double totalRewards = 0;
        for (MirCertificate mirCertificate : mirCertificates) {
            if (mirCertificate.getPot() == MirPot.TREASURY) {
                totalRewards += mirCertificate.getTotalRewards();
            }
        }

        Assertions.assertEquals(totalRewards, 3.9432064006444E13);
    }

    @Test
    public void testGetRetiredPoolsInEpoch218() {
        int epoch = 218;
        List<PoolDeregistration> poolDeregistrations = dbSyncDataProvider.getRetiredPoolsInEpoch(epoch);

        Assertions.assertEquals(7, poolDeregistrations.size());
        Assertions.assertTrue(poolDeregistrations.stream().anyMatch(poolDeregistration -> poolDeregistration.getPoolId().equals("pool1k5fu9x0sv47swapw9py3r4zf52tvpm77nnxvee75aaw0kew00t5")));
        Assertions.assertTrue(poolDeregistrations.stream().anyMatch(poolDeregistration -> poolDeregistration.getPoolId().equals("pool1fj5jf9ey6r2j98s98qtrjvlnrxr9nx6dx7x8avnv5t5gk53lve9")));
        Assertions.assertTrue(poolDeregistrations.stream().anyMatch(poolDeregistration -> poolDeregistration.getPoolId().equals("pool13qhkchlzakkjf4aaxj5ma2d5fcu9yr99ednxg75p45ctzdgrw95")));
        Assertions.assertTrue(poolDeregistrations.stream().anyMatch(poolDeregistration -> poolDeregistration.getPoolId().equals("pool1vgr2sfve7lj3dx4j7wzfl5r22zqrylxxxpg4c4g937ksydlyy5e")));
        Assertions.assertTrue(poolDeregistrations.stream().anyMatch(poolDeregistration -> poolDeregistration.getPoolId().equals("pool12cr629pk4ppsc947d9ut2s7rvwadx50tklcnqyh306xvyz6tq42")));
        Assertions.assertTrue(poolDeregistrations.stream().anyMatch(poolDeregistration -> poolDeregistration.getPoolId().equals("pool1ech9hwhqe2j3gecdv08a45cj8fwn9nmuxl0c0tw45rm47hluyng")));
        Assertions.assertTrue(poolDeregistrations.stream().anyMatch(poolDeregistration -> poolDeregistration.getPoolId().equals("pool1s679peru5lc03a9urdvkntsc3snfjnuwfc7vh42lnu395af0m9v")));
    }

    @Test
    public void testGetRetiredPoolsInEpoch210() {
        int epoch = 210;
        List<PoolDeregistration> poolDeregistrations = dbSyncDataProvider.getRetiredPoolsInEpoch(epoch);
        List<String> expectedPoolIds = List.of(
                "pool1r4rwd64q8xk36m6w2vsq48zfcmfxj0dxypcqdpmy2hnwg9vwv59",
                "pool1cact6224505keg4a4lysleec0mq5886rlzcr92q5ndftuntfe0y",
                "pool10gllgflzl3ngg0fj2xas9hfww25zxn2fdwtc0gd44am92pdlr7p",
                "pool16veyvjd43dxaccwvvr4zq5s6cw7me2kgpmsyf2qel0l266kattr",
                "pool1lj9t0saaxejj6drmxefgejwuy47lx4ewwruwvn3uttyuw6f5389",
                "pool10hz2at4lstff7n8ej4a02jf4fyy4h6glp9y6328kg6ctsn83fes",
                "pool1clqktqscpp3zl5ps8vlatqx0vyn59ls6n9453at955mkcxx9kfv",
                "pool1dkalquvvf6hxnr2sxwska29n78ynxndlld3qad8w9mcj2u3hgpz",
                "pool1urjhx4j3v3phllg3uups6gnf2xuchsm3qpy4z75dx7grw5nt46f",
                "pool1cldam492zgdw8tvay70f7yzre89syp74phgtapl5wtlzylnd20a",
                "pool15ud5hthetfx6avs3ulm0w8gwga75p565zpphruyjl4k7sq3phlm",
                "pool1dm79803lhmu9h32vsg3cup5djkme9c3vjlngjx220tmkg78c305",
                "pool1nxjn62rdwqwefyaeq2mrz8r5vrekh6y43qn6nqsl3f0fzx4jpxf",
                "pool1cnfdwkjgjd84jtd9sgajhf8wp7ak45j5uj7wydzjpy927fj86xf");

        Assertions.assertEquals(expectedPoolIds.size(), poolDeregistrations.size());
        Assertions.assertTrue(poolDeregistrations.stream().allMatch(poolDeregistration -> expectedPoolIds.contains(poolDeregistration.getPoolId())));
    }

    @Test
    public void testGetAccountUpdatesUntilEpoch() {
        int epoch = 211;
        List<String> poolStakeAddresses = List.of("stake1uykca6g5lwpmfs55wv28mqrt63nucqxpch63jx4srzgmx2grwlrgw");
        List<AccountUpdate> accountUpdates = dbSyncDataProvider.getAccountUpdatesUntilEpoch(poolStakeAddresses, epoch);
        Assertions.assertEquals(accountUpdates.size(), 1);
    }

    static Stream<Integer> dataProviderRangeUntilEpoch225() {
        return IntStream.range(210, 225).boxed();
    }

    @ParameterizedTest
    @MethodSource("dataProviderRangeUntilEpoch225")
    void Test_calculateTreasuryWithDbSyncDataProvider(final int epoch) {
        TreasuryCalculationResult treasuryCalculationResult = TreasuryCalculation.calculateTreasuryForEpoch(epoch, dbSyncDataProvider);

        double difference = treasuryCalculationResult.getActualTreasury() - treasuryCalculationResult.getCalculatedTreasury();
        System.out.println(difference);
        Assertions.assertTrue(Math.abs(difference) < 1, "The difference " + lovelaceToAda(difference) + " ADA between expected treasury value and actual treasury value is greater than 1 LOVELACE");
    }

    private void Test_calculatePoolRewardWithDbSyncDataProvider(final String poolId,
                                          final int epoch) {
        PoolRewardCalculationResult poolRewardCalculationResult =
                PoolRewardCalculation.calculatePoolRewardInEpoch(poolId, epoch, dbSyncDataProvider);

        PoolHistory poolHistoryCurrentEpoch = dbSyncDataProvider.getPoolHistory(poolId, epoch);
        if (poolHistoryCurrentEpoch == null) {
            Assertions.assertEquals(0.0, poolRewardCalculationResult.getPoolReward());
            return;
        }

        double difference = (poolHistoryCurrentEpoch.getDelegatorRewards() - (poolRewardCalculationResult.getPoolReward() - poolRewardCalculationResult.getPoolFee()));
        Assertions.assertTrue(Math.abs(difference) < adaToLovelace(1), "The difference between expected pool reward and actual pool reward is greater than 1 ADA: " + lovelaceToAda(difference) + " ADA");
    }

    static Stream<String> testPoolIds() {
        return Stream.of(
                "pool1qqqqx69ztfvd83rtafs3mk4lwanehrglmp2vwkjpaguecs2t4c2",
                "pool19ctjr5ft75sz396hn0tf6ns4hy5w9l9jp2jh3m8mx6acvm2cn7j",
                "pool1xxhs2zw5xa4g54d5p62j46nlqzwp8jklqvuv2agjlapwjx9qkg9",
                "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt",
                "pool12t3zmafwjqms7cuun86uwc8se4na07r3e5xswe86u37djr5f0lx",
                "pool1spus7k8cy5qcs82xhw60dwwk2d4vrfs0m5vr2zst04gtq700gjn",
                "pool13n4jzw847sspllczxgnza7vkq80m8px7mpvwnsqthyy2790vmyc",
                "pool1ljlmfg7p37ysmea9ra5xqwccue203dpj40w6zlzn5r2cvjrf6tw"
        );
    }

    @ParameterizedTest
    @MethodSource("testPoolIds")
    void calculatePoolRewardInEpoch213(String poolId) {
        int epoch = 213;
        Test_calculatePoolRewardWithDbSyncDataProvider(poolId, epoch);
    }
}
