package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.AdaPots;
import org.cardanofoundation.rewards.entity.ProtocolParameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootTest
@ComponentScan
public class JsonDataProviderTest {

    @Autowired
    JsonDataProvider jsonDataProvider;

    @Test
    void test_getAdaPotsForEpoch220() {
        AdaPots adaPots = jsonDataProvider.getAdaPotsForEpoch(220);
        Assertions.assertEquals(adaPots.getEpoch(), 220);
        Assertions.assertEquals(adaPots.getTreasury(), 94812346026398.0);
        Assertions.assertEquals(adaPots.getReserves(), 13120582265809833.0);
        Assertions.assertEquals(adaPots.getRewards(), 151012138061367.0);
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
