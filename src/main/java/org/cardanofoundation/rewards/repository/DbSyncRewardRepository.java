package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.entity.jpa.DbSyncReward;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncRewardRepository extends ReadOnlyRepository<DbSyncReward, Long>{

    @Query("""
           SELECT SUM(reward.amount) from DbSyncReward AS reward
             	WHERE reward.pool.bech32PoolId = :poolId
             	AND reward.earnedEpoch = :epoch
            """)
    Double getRewardsForPoolInEpoch(String poolId, Integer epoch);

    @Query("""
           SELECT reward from DbSyncReward AS reward
             	WHERE (reward.type = 'reserves' OR reward.type = 'treasury')
             	AND reward.earnedEpoch = :epoch
            """)
    List<DbSyncReward> getMIRCertificatesInEpoch(Integer epoch);

    @Query("""
           SELECT reward from DbSyncReward AS reward
             	WHERE reward.stakeAddress.view IN :addresses
             	AND reward.earnedEpoch = :epoch
            """)
    List<DbSyncReward> getRewardsForAddressesInEpoch(List<String> addresses, Integer epoch);
}
