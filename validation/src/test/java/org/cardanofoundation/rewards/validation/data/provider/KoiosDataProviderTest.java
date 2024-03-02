package org.cardanofoundation.rewards.validation.data.provider;

import org.cardanofoundation.rewards.calculation.domain.Epoch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootTest
@ComponentScan
public class KoiosDataProviderTest {

    @Autowired
    KoiosDataProvider koiosDataProvider;

    @Test
    void Test_getEpochInfoForEpoch208() {
        Epoch epochInfo = koiosDataProvider.getEpochInfo(208);
        Assertions.assertEquals(epochInfo.getNumber(), 208);
        Assertions.assertEquals(epochInfo.getBlockCount(), 21556);
        Assertions.assertEquals(epochInfo.getNonOBFTBlockCount(), 0);
        Assertions.assertEquals(epochInfo.getOBFTBlockCount(), 21556);
    }

    @Test
    void Test_getEpochInfoForEpoch210() {
        Epoch epochInfo = koiosDataProvider.getEpochInfo(210);
        Assertions.assertEquals(epochInfo.getNumber(), 210);
        Assertions.assertEquals(epochInfo.getBlockCount(), 21547);
        Assertions.assertEquals(epochInfo.getNonOBFTBlockCount(), 0);
        Assertions.assertEquals(epochInfo.getOBFTBlockCount(), 21547);
    }

    @Test
    void Test_getEpochInfoForEpoch211() {
        Epoch epochInfo = koiosDataProvider.getEpochInfo(211);
        Assertions.assertEquals(epochInfo.getNumber(), 211);
        Assertions.assertEquals(epochInfo.getBlockCount(), 21315);
        Assertions.assertEquals(epochInfo.getNonOBFTBlockCount(), 1991);
        Assertions.assertEquals(epochInfo.getOBFTBlockCount(), 19324);
        Assertions.assertEquals(epochInfo.getNonOBFTBlockCount() + epochInfo.getOBFTBlockCount(),
                epochInfo.getBlockCount());
    }

    @Test
    void Test_getEpochInfoForEpoch230() {
        Epoch epochInfo = koiosDataProvider.getEpochInfo(230);
        Assertions.assertEquals(epochInfo.getNumber(), 230);
        Assertions.assertEquals(epochInfo.getBlockCount(), 21244);
        Assertions.assertEquals(epochInfo.getNonOBFTBlockCount(), 11902);
        Assertions.assertEquals(epochInfo.getOBFTBlockCount(), 9342);
        Assertions.assertEquals(epochInfo.getNonOBFTBlockCount() + epochInfo.getOBFTBlockCount(),
                epochInfo.getBlockCount());
    }

    @Test
    void Test_getEpochInfoForEpoch256() {
        Epoch epochInfo = koiosDataProvider.getEpochInfo(256);
        Assertions.assertEquals(epochInfo.getNumber(), 256);
        Assertions.assertEquals(epochInfo.getBlockCount(), 21059);
        Assertions.assertEquals(epochInfo.getNonOBFTBlockCount(), 20640);
        Assertions.assertEquals(epochInfo.getOBFTBlockCount(), 419);
        Assertions.assertEquals(epochInfo.getNonOBFTBlockCount() + epochInfo.getOBFTBlockCount(),
                epochInfo.getBlockCount());
    }

    @Test
    void Test_getEpochInfoForEpoch257() {
        Epoch epochInfo = koiosDataProvider.getEpochInfo(257);
        Assertions.assertEquals(epochInfo.getNumber(), 257);
        Assertions.assertEquals(epochInfo.getBlockCount(), 20836);
        Assertions.assertEquals(epochInfo.getNonOBFTBlockCount(), 20836);
        Assertions.assertEquals(epochInfo.getOBFTBlockCount(), 0);
        Assertions.assertEquals(epochInfo.getNonOBFTBlockCount() + epochInfo.getOBFTBlockCount(),
                epochInfo.getBlockCount());
    }
}
