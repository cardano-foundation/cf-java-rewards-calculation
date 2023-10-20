package org.cardanofoundation.rewards.data.provider;

import org.cardanofoundation.rewards.entity.Epoch;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
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
}
