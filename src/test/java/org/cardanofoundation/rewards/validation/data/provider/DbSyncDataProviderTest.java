package org.cardanofoundation.rewards.validation.data.provider;

import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.PoolRewardValidation;
import org.cardanofoundation.rewards.validation.TreasuryValidation;
import org.cardanofoundation.rewards.calculation.enums.MirPot;
import org.cardanofoundation.rewards.validation.data.provider.DbSyncDataProvider;
import org.cardanofoundation.rewards.validation.domain.TreasuryValidationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.cardanofoundation.rewards.calculation.util.CurrencyConverter.lovelaceToAda;

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
        Assertions.assertEquals(epoch.getFees(), new BigInteger("5135934788"));
        Assertions.assertEquals(epoch.getBlockCount(), 21627);
    }

    @Test
    public void testGetEpochInfoOf215() {
        Epoch epoch = dbSyncDataProvider.getEpochInfo(215);
        Assertions.assertEquals(epoch.getNumber(), 215);
        Assertions.assertEquals(epoch.getFees(), new BigInteger("8110049274"));
        Assertions.assertEquals(epoch.getBlockCount(), 21572);
        Assertions.assertEquals(epoch.getNonOBFTBlockCount(), 5710);
        Assertions.assertEquals(epoch.getUnixTimeFirstBlock(), 1599083091);
        Assertions.assertEquals(epoch.getUnixTimeLastBlock(), 1599515063);
        Assertions.assertEquals(epoch.getOBFTBlockCount(), 15862);
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
        Assertions.assertEquals(poolHistory.getActiveStake(), new BigInteger("27523186299296"));
        Assertions.assertEquals(poolHistory.getBlockCount(), 10);
        Assertions.assertEquals(poolHistory.getFixedCost(), 340000000.0);
        Assertions.assertEquals(poolHistory.getMargin(), 0.009);
        Assertions.assertEquals(poolHistory.getDelegatorRewards(), new BigInteger("14877804008"));
        Assertions.assertEquals(poolHistory.getPoolFees(), new BigInteger("475116283"));
    }

    @Test
    public void testGetHistoryOfPoolOwnersInEpoch() {
        String poolId = "pool1z5uqdk7dzdxaae5633fqfcu2eqzy3a3rgtuvy087fdld7yws0xt";
        int epoch = 220;
        PoolOwnerHistory poolOwnerHistory = dbSyncDataProvider.getHistoryOfPoolOwnersInEpoch(poolId, epoch);
        Assertions.assertEquals(poolOwnerHistory.getActiveStake(), new BigInteger("476793511093"));
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
}
