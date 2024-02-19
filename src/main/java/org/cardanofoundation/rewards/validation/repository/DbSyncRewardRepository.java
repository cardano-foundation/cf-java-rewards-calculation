package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncReward;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.MemberReward;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.TotalPoolRewards;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.util.List;

@Repository
@Profile("db-sync")
public interface DbSyncRewardRepository extends ReadOnlyRepository<DbSyncReward, Long>{

    @Query(nativeQuery = true, value = """
            SELECT pool_hash.view AS poolId, amount, stake_address.view AS stakeAddress FROM reward
            JOIN pool_hash ON pool_hash.id=reward.pool_id
            JOIN stake_address ON stake_address.id=reward.addr_id
            WHERE earned_epoch=:epoch AND pool_id IS NOT NULL AND type='member';
            """)
    List<MemberReward> getMemberRewardsInEpoch(Integer epoch);

    @Query("""
           SELECT SUM(reward.amount) from DbSyncReward AS reward
             	WHERE reward.pool.bech32PoolId = :poolId
             	AND reward.earnedEpoch = :epoch AND (reward.type = 'member' OR reward.type = 'leader')
            """)
    BigInteger getTotalPoolRewardsInEpoch(String poolId, Integer epoch);

    @Query("""
           SELECT reward from DbSyncReward AS reward
             	WHERE (reward.type = 'reserves' OR reward.type = 'treasury')
             	AND reward.earnedEpoch = :epoch
            """)
    List<DbSyncReward> getMIRCertificatesInEpoch(Integer epoch);

    @Query(nativeQuery = true, value = """
            SELECT pool_hash.view AS poolId, SUM(amount) AS amount FROM reward
                JOIN pool_hash ON pool_hash.id=reward.pool_id
            WHERE earned_epoch=:epoch AND pool_id IS NOT NULL AND (type='member' OR type='leader') GROUP BY pool_id, pool_hash.view
            """)
    List<TotalPoolRewards> getSumOfMemberAndLeaderRewardsInEpoch(Integer epoch);

    @Query("""
           SELECT reward from DbSyncReward AS reward
             	WHERE reward.stakeAddress.view IN :addresses
             	AND reward.earnedEpoch = :epoch
            """)
    List<DbSyncReward> getRewardsForAddressesInEpoch(List<String> addresses, Integer epoch);
}
