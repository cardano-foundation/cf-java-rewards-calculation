package org.cardanofoundation.rewards.service;

import java.util.Optional;

import org.cardanofoundation.rewards.common.entity.EpochParam;

public interface EpochParamService {
  Optional<EpochParam> getEpochParam(int epoch);
}
