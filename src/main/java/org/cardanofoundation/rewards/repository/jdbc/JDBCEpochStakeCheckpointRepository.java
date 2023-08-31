package org.cardanofoundation.rewards.repository.jdbc;

import java.util.List;

import org.cardanofoundation.rewards.common.entity.EpochStakeCheckpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("koios")
public class JDBCEpochStakeCheckpointRepository {

  private final JdbcTemplate jdbcTemplate;

  private final int batchSize;

  public JDBCEpochStakeCheckpointRepository(
      JdbcTemplate jdbcTemplate,
      @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}") int batchSize) {
    this.jdbcTemplate = jdbcTemplate;
    this.batchSize = batchSize;
  }

  @Transactional
  public void saveAll(List<EpochStakeCheckpoint> epochStakeCheckpoints) {
    String sql =
        "INSERT INTO epoch_stake_checkpoint (id, view, epoch_checkpoint) "
            + " VALUES (nextval('epoch_stake_checkpoint_id_seq'), ?, ?)"
            + "    ON CONFLICT (view) DO NOTHING";

    jdbcTemplate.batchUpdate(
        sql,
        epochStakeCheckpoints,
        batchSize,
        (ps, epochStakeCheckpoint) -> {
          ps.setString(1, epochStakeCheckpoint.getStakeAddress());
          ps.setLong(2, epochStakeCheckpoint.getEpochCheckpoint());
        });
  }
}
