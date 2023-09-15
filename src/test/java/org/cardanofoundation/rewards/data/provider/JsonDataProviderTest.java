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
    void test_getAdaPotsForEpoch209() {
        AdaPots adaPots = jsonDataProvider.getAdaPotsForEpoch(209);
        Assertions.assertEquals(adaPots.getEpoch(), 209);
        Assertions.assertEquals(adaPots.getTreasury(), 8332813711755.0);
        Assertions.assertEquals(adaPots.getReserves(), 13286160713028443.0);
        Assertions.assertEquals(adaPots.getRewards(), 593536826186446.0);
    }

    @Test
    void test_getProtocolParametersForEpoch209() {
        ProtocolParameters protocolParameters = jsonDataProvider.getProtocolParametersForEpoch(209);
        Assertions.assertEquals(protocolParameters.getDecentralisation(), 0.2);
        Assertions.assertEquals(protocolParameters.getMonetaryExpandRate(), 0.003);
        Assertions.assertEquals(protocolParameters.getOptimalPoolCount(), 150);
        Assertions.assertEquals(protocolParameters.getPoolOwnerInfluence(), 0.03);
    }

}
