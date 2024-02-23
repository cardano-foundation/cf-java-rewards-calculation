package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.calculation.domain.AdaPots;
import org.cardanofoundation.rewards.calculation.domain.ProtocolParameters;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import java.math.BigInteger;

@SpringBootTest
@ComponentScan
public class JsonDataProviderTest {

    @Autowired
    JsonDataProvider jsonDataProvider;

    @Test
    void test_getAdaPotsForEpoch220() {
        AdaPots adaPots = jsonDataProvider.getAdaPotsForEpoch(220);
        Assertions.assertEquals(adaPots.getEpoch(), 220);
        Assertions.assertEquals(adaPots.getTreasury(), new BigInteger("94812346026398"));
        Assertions.assertEquals(adaPots.getReserves(), new BigInteger("13120582265809832"));
        Assertions.assertEquals(adaPots.getRewards(), new BigInteger("151012138061367"));
    }

    @Test
    void test_getProtocolParametersForEpoch209() {
        ProtocolParameters protocolParameters = jsonDataProvider.getProtocolParametersForEpoch(209);
        Assertions.assertEquals(protocolParameters.getDecentralisation(), 1.0);
        Assertions.assertEquals(protocolParameters.getMonetaryExpandRate(), 0.003);
        Assertions.assertEquals(protocolParameters.getOptimalPoolCount(), 150);
        Assertions.assertEquals(protocolParameters.getPoolOwnerInfluence(), 0.3);
    }

}
