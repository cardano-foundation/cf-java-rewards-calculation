package org.cardanofoundation.rewards.repository.jdbc;

import java.util.List;

import org.cardanofoundation.rewards.common.entity.RewardCheckpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("koios")
public class JDBCRewardCheckpointRepository {

  private final JdbcTemplate jdbcTemplate;

  private int batchSize;

  public JDBCRewardCheckpointRepository(
      JdbcTemplate jdbcTemplate,
      @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}") int batchSize) {
    this.jdbcTemplate = jdbcTemplate;
    this.batchSize = batchSize;
  }

  @Transactional
  public void saveAll(List<RewardCheckpoint> rewardCheckpoints) {
    String sql =
        "INSERT INTO reward_checkpoint (id, view, epoch_checkpoint) "
            + " VALUES (nextval('reward_checkpoint_id_seq'), ?, ?)"
            + "    ON CONFLICT (view) DO NOTHING";

    jdbcTemplate.batchUpdate(
        sql,
        rewardCheckpoints,
        batchSize,
        (ps, rewardCheckpoint) -> {
          ps.setString(1, rewardCheckpoint.getStakeAddress());
          ps.setLong(2, rewardCheckpoint.getEpochCheckpoint());
        });
  }
}
