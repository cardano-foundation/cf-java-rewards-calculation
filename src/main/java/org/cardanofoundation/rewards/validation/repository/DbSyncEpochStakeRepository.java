package org.cardanofoundation.rewards.validation.repository;

import org.cardanofoundation.rewards.calculation.domain.Delegator;
import org.cardanofoundation.rewards.validation.entity.jpa.DbSyncEpochStake;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.PoolEpochStake;
import org.springframework.data.jpa.repository.Query;

import java.math.BigInteger;
import java.util.List;

public interface DbSyncEpochStakeRepository extends ReadOnlyRepository<DbSyncEpochStake, Long> {

    @Query("""
        SELECT es.amount AS amount, es.epoch AS epoch, es.pool.bech32PoolId as poolId, es.stakeAddress.view as stakeAddress FROM DbSyncEpochStake AS es
        WHERE es.epoch=:epoch AND es.pool.bech32PoolId=:poolId""")
    List<PoolEpochStake> getPoolActiveStakeInEpoch(String poolId, Integer epoch);

    @Query(nativeQuery = true, value = """
            SELECT
                amount, epoch_no AS epoch, pool_hash.view AS poolId, stake_address.view as stakeAddress
            FROM epoch_stake
            	JOIN pool_hash ON pool_hash.id=epoch_stake.pool_id
            	JOIN stake_address ON stake_address.id = epoch_stake.addr_id
            WHERE epoch_no=:epoch AND pool_hash.view IN :poolIds AND amount > 0""")
    List<PoolEpochStake> getAllPoolsActiveStakesInEpoch(Integer epoch, List<String> poolIds);

    @Query("SELECT SUM(amount) FROM DbSyncEpochStake WHERE epoch=:epoch")
    BigInteger getEpochStakeByEpoch(Integer epoch);
}
