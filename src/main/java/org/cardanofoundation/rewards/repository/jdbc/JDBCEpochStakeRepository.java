package org.cardanofoundation.rewards.repository.jdbc;

import java.sql.Types;
import java.util.Collection;

import org.cardanofoundation.rewards.common.entity.EpochStake;
import org.cardanofoundation.rewards.util.CommonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JDBCEpochStakeRepository {

  private final JdbcTemplate jdbcTemplate;

  private final int batchSize;

  public JDBCEpochStakeRepository(
      JdbcTemplate jdbcTemplate,
      @Value("${spring.jpa.properties.hibernate.jdbc.batch_size}") int batchSize) {
    this.jdbcTemplate = jdbcTemplate;
    this.batchSize = batchSize;
  }

  @Transactional
  public void saveAll(Collection<EpochStake> epochStakeList) {
    String sql =
        "INSERT INTO epoch_stake (id, epoch_no, amount, addr_id, pool_id)"
            + " VALUES (nextval('epoch_stake_id_seq'), ?, ?, ?, ?)";

    jdbcTemplate.batchUpdate(
        sql,
        epochStakeList,
        batchSize,
        (ps, epochStake) -> {
          CommonUtils.setNullableValue(ps, 1, epochStake.getEpochNo(), Types.INTEGER);
          CommonUtils.setNullableValue(ps, 2, epochStake.getAmount(), Types.BIGINT);

          if (epochStake.getAddr() != null) {
            ps.setLong(3, epochStake.getAddr().getId());
          } else {
            ps.setNull(3, Types.BIGINT);
          }

          if (epochStake.getPool() != null) {
            ps.setLong(4, epochStake.getPool().getId());
          } else {
            ps.setNull(4, Types.BIGINT);
          }
        });
  }
}
