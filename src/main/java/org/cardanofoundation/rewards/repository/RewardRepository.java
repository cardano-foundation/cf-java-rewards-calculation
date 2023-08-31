package org.cardanofoundation.rewards.repository;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import org.cardanofoundation.rewards.common.entity.Reward;
import org.cardanofoundation.rewards.common.entity.RewardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.cardanofoundation.rewards.projection.DelegatorReceivedRewardProjection;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {
  @Query("SELECT SUM(reward.amount) FROM Reward reward WHERE reward.spendableEpoch <= :epoch")
  BigInteger getTotalRewardToEpoch(@Param("epoch") int epoch);

  @Query(
      "SELECT SUM(reward.amount) FROM Reward reward "
          + "WHERE reward.earnedEpoch = :epoch And reward.type = :type")
  BigInteger getTotalRewardOfEpochByType(@Param("epoch") int epoch, @Param("type") RewardType type);

  @Query(
      "SELECT SUM(reward.amount) FROM Reward reward "
          + "WHERE reward.spendableEpoch = :epoch And reward.type != org.cardanofoundation.rewards.common.entity.RewardType.REFUND")
  BigInteger getTotalSpendableRewardExcludeRefund(@Param("epoch") int epoch);

  @Query(
      "SELECT reward.poolId FROM Reward reward "
          + "WHERE reward.earnedEpoch = :epoch "
          + "AND reward.type IN :rewardTypes "
          + "AND reward.amount > 0")
  Set<Long> getPoolIdHasRewardInAnEpochByTypes(
      @Param("epoch") int epoch, @Param("rewardTypes") Set<RewardType> rewardTypes);

  @Query(
      "SELECT reward FROM Reward reward "
          + "WHERE reward.earnedEpoch = :epoch "
          + "AND reward.type IN :rewardTypes")
  List<Reward> getRewardInAnEpochByTypes(
      @Param("epoch") int epoch, @Param("rewardTypes") Set<RewardType> rewardTypes);

  @Query(
      "SELECT new org.cardanofoundation.rewards.projection.DelegatorReceivedRewardProjection(rwd.stakeAddressId, SUM(rwd.amount)) "
          + "FROM Reward rwd "
          + "WHERE rwd.spendableEpoch <= :epochNo "
          + "OR (rwd.spendableEpoch <= (:epochNo + 1) "
          + "AND rwd.type IN (org.cardanofoundation.rewards.common.entity.RewardType.TREASURY,"
          + "org.cardanofoundation.rewards.common.entity.RewardType.RESERVES)) "
          + "OR (rwd.earnedEpoch <= :epochNo - 1"
          + "AND rwd.type IN (org.cardanofoundation.rewards.common.entity.RewardType.LEADER,"
          + "org.cardanofoundation.rewards.common.entity.RewardType.MEMBER)) "
          + "GROUP BY rwd.stakeAddressId")
  List<DelegatorReceivedRewardProjection> findTotalReceivedRewardsTilEpochNo(
      @Param("epochNo") int epochNo);
}
