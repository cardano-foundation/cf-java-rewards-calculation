package org.cardanofoundation.rewards.repository.jdbc;

import java.sql.Types;
import java.util.Collection;

import org.cardanofoundation.rewards.common.entity.Reward;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JDBCRewardRepository {

  private final JdbcTemplate jdbcTemplate;

  private int batchSize;

  public JDBCRewardRepository(
      JdbcTemplate jdbcTemplate,
      @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}") int batchSize) {
    this.jdbcTemplate = jdbcTemplate;
    this.batchSize = batchSize;
  }

  @Transactional
  public void saveAll(Collection<Reward> rewards) {
    String sql =
        "INSERT INTO reward (id, type, amount, earned_epoch, spendable_epoch, addr_id, "
            + "pool_id)"
            + " VALUES (nextval('reward_id_seq'), ?, ?, ?, ?, ?, ?)";
    jdbcTemplate.batchUpdate(
        sql,
        rewards,
        batchSize,
        (ps, reward) -> {
          ps.setString(1, reward.getType().getValue());
          ps.setLong(2, reward.getAmount().longValue());
          ps.setLong(3, reward.getEarnedEpoch());
          ps.setLong(4, reward.getSpendableEpoch());
          if (reward.getAddr() != null) {
            ps.setLong(5, reward.getAddr().getId());
          } else {
            ps.setNull(5, Types.BIGINT);
          }
          if (reward.getPool() != null) {
            ps.setLong(6, reward.getPool().getId());
          } else {
            ps.setNull(6, Types.BIGINT);
          }
        });
  }
}
