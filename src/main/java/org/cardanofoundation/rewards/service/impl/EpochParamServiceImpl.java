package org.cardanofoundation.rewards.service.impl;

import java.util.Optional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.service.EpochParamService;
import org.springframework.stereotype.Service;

import org.cardanofoundation.rewards.common.entity.EpochParam;
import org.cardanofoundation.rewards.repository.EpochParamRepository;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class EpochParamServiceImpl implements EpochParamService {

  EpochParamRepository epochParamRepository;

  @Override
  public Optional<EpochParam> getEpochParam(int epoch) {
    return epochParamRepository.getEpochParamByEpochNo(epoch);
  }
}
