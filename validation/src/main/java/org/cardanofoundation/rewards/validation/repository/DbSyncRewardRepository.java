package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.validation.entity.dbsync.DbSyncReward;
import org.cardanofoundation.rewards.validation.entity.projection.MemberReward;
import org.cardanofoundation.rewards.validation.entity.projection.MirTransition;
import org.cardanofoundation.rewards.validation.entity.projection.TotalPoolRewards;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
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
    HashSet<MemberReward> getMemberRewardsInEpoch(@Param("epoch") Integer epoch);

    @Query(nativeQuery = true, value = """
            SELECT SUM(amount) AS totalRewards, type AS pot
            FROM reward WHERE (reward.type = 'reserves' OR reward.type = 'treasury')
            AND earned_epoch = :epoch GROUP BY type
            """)
    List<MirTransition> getMIRCertificatesInEpoch(@Param("epoch") Integer epoch);

    @Query(nativeQuery = true, value = """
            SELECT pool_hash.view AS poolId, SUM(amount) AS amount FROM reward
                JOIN pool_hash ON pool_hash.id=reward.pool_id
            WHERE earned_epoch=:epoch AND pool_id IS NOT NULL AND (type='member' OR type='leader') GROUP BY pool_id, pool_hash.view
            """)
    HashSet<TotalPoolRewards> getSumOfMemberAndLeaderRewardsInEpoch(@Param("epoch") Integer epoch);
}
