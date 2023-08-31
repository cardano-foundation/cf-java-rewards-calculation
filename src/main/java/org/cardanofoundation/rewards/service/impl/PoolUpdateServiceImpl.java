package org.cardanofoundation.rewards.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.projection.PoolConfigProjection;
import org.cardanofoundation.rewards.service.PoolRetireService;
import org.cardanofoundation.rewards.service.PoolUpdateService;
import org.springframework.stereotype.Service;

import org.cardanofoundation.rewards.repository.PoolUpdateRepository;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Service
public class PoolUpdateServiceImpl implements PoolUpdateService {

  PoolRetireService poolRetireService;
  PoolUpdateRepository poolUpdateRepository;

  @Override
  public List<PoolConfigProjection> findAllActivePoolConfig(int epoch) {
    var poolRetiredIds = poolRetireService.getPoolRetiredIdTilEpoch(epoch);
    log.info("Epoch {}, pool ids size {}", epoch, poolRetiredIds.size());
    var poolConfig = poolUpdateRepository.findAllActivePoolConfig(epoch, poolRetiredIds);
    return getLatestPoolConfiguration(poolConfig, epoch);
  }

  @Override
  public List<PoolConfigProjection> findPoolHasMintedBlockInEpoch(int epoch) {
    var poolConfig = poolUpdateRepository.findAllPoolConfigHasMintedBlockInEpoch(epoch);
    return getLatestPoolConfiguration(poolConfig, epoch);
  }

  // make sure that even in the same tx has many pool update of the same certificate
  // then we will take the latest pool update has certificate index is biggest
  private List<PoolConfigProjection> getLatestPoolConfiguration(
      List<PoolConfigProjection> poolConfigProjections, int epoch) {
    return new ArrayList<>(
        poolConfigProjections.stream()
            .collect(
                Collectors.toMap(
                    PoolConfigProjection::getPoolId,
                    Function.identity(),
                    (a, b) -> {
                      if (a.getActivateEpochNo() > b.getActivateEpochNo()
                          && a.getActivateEpochNo() <= epoch) {
                        return a;
                      } else if (b.getActivateEpochNo() > a.getActivateEpochNo()
                          && b.getActivateEpochNo() <= epoch) {
                        return b;
                      } else if (a.getTxId() > b.getTxId()) {
                        return a;
                      } else {
                        if (a.getCertIndex() > b.getCertIndex()) return a;
                        return b;
                      }
                    }))
            .values());
  }
}
