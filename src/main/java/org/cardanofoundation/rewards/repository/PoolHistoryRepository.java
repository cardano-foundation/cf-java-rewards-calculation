package org.cardanofoundation.rewards.repository;

import org.cardanofoundation.rewards.common.entity.PoolHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoolHistoryRepository extends JpaRepository<PoolHistory, Long> {}
