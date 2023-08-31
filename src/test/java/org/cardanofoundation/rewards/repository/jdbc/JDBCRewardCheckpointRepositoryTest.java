package org.cardanofoundation.rewards.repository.jdbc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.cardanofoundation.rewards.common.entity.RewardCheckpoint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class JDBCRewardCheckpointRepositoryTest {

  @Mock private JdbcTemplate jdbcTemplate;

  private JDBCRewardCheckpointRepository jdbcRewardCheckpointRepository;

  private final int batchSize = 100;

  @BeforeEach
  void setUp() {
    jdbcRewardCheckpointRepository = new JDBCRewardCheckpointRepository(jdbcTemplate, batchSize);
  }

  @Test
  void testSaveAll() {
    // Setup
    var checkpoint1 =
        new RewardCheckpoint("stake1uxgfzz027y0scn8pqh220vk08s0nc74plnrl6wmr5nve2lqt5mfls", 414);
    var checkpoint2 =
        new RewardCheckpoint("stake1u8ludg6vutasqf7sfzwp73tw4ukszy7kwqfk7hd52yq9frs528563", 414);

    final List<RewardCheckpoint> rewardCheckpoints = List.of(checkpoint1, checkpoint2);

    // Run the test
    jdbcRewardCheckpointRepository.saveAll(rewardCheckpoints);

    // Verify the results
    verify(jdbcTemplate)
        .batchUpdate(
            eq(
                "INSERT INTO reward_checkpoint (id, view, epoch_checkpoint)  VALUES (nextval('reward_checkpoint_id_seq'), ?, ?)    ON CONFLICT (view) DO NOTHING"),
            eq(rewardCheckpoints),
            eq(batchSize),
            any(ParameterizedPreparedStatementSetter.class));
  }
}
