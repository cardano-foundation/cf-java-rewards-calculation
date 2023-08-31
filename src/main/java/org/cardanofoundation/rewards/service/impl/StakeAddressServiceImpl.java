package org.cardanofoundation.rewards.service.impl;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.constants.RewardConstants;
import org.cardanofoundation.rewards.projection.StakeIdTxIdProjection;
import org.cardanofoundation.rewards.service.StakeAddressService;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;

import org.cardanofoundation.rewards.repository.StakeDeregistrationRepository;
import org.cardanofoundation.rewards.repository.StakeRegistrationRepository;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Service
public class StakeAddressServiceImpl implements StakeAddressService {

  StakeRegistrationRepository stakeRegistrationRepository;
  StakeDeregistrationRepository stakeDeregistrationRepository;

  @Override
  public Set<Long> getStakeAddressRegisteredTilEpoch(long txId, Set<Long> stakeAddrIds, int epoch) {
    Set<Long> stAddrRegisteredIds = new ConcurrentSkipListSet<>();
    var queryBatches = Lists.partition(new ArrayList<>(stakeAddrIds), RewardConstants.BATCH_QUERY_SIZE);
    queryBatches.parallelStream()
        .forEach(
            ids -> {
              var addrsRegistered =
                  stakeRegistrationRepository.getLastCertificateStakeIdAndTxIdByEpoch(
                      epoch, ids, txId);
              var addrsDeregistered =
                  stakeDeregistrationRepository
                      .getLastDeregistrationCertificateStakeIdAndTxIdByEpoch(epoch, ids, txId);
              stAddrRegisteredIds.addAll(
                  getAddrIdNotDeregistered(addrsRegistered, addrsDeregistered));
            });
    return stAddrRegisteredIds;
  }

  private Set<Long> getAddrIdNotDeregistered(
          List<StakeIdTxIdProjection> addrsRegistered, List<StakeIdTxIdProjection> addrsDeregistered) {
    var mAddrsDeregistered = convertStakeIdAndTxIdToMap(addrsDeregistered);
    return addrsRegistered.stream()
        .filter(
            stakeIdTxId -> {
              var deregisteredTxId = mAddrsDeregistered.get(stakeIdTxId.getStakeAddressId());
              if (Objects.nonNull(deregisteredTxId)) {
                return deregisteredTxId < stakeIdTxId.getTxId();
              }
              return true;
            })
        .map(StakeIdTxIdProjection::getStakeAddressId)
        .collect(Collectors.toSet());
  }

  private Map<Long, Long> convertStakeIdAndTxIdToMap(List<StakeIdTxIdProjection> addrTxs) {
    return addrTxs.stream()
        .collect(
            Collectors.toMap(
                StakeIdTxIdProjection::getStakeAddressId, StakeIdTxIdProjection::getTxId));
  }
}
