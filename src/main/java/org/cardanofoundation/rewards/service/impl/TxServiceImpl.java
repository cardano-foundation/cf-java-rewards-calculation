package org.cardanofoundation.rewards.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.service.TxService;
import org.springframework.stereotype.Service;

import org.cardanofoundation.rewards.repository.BlockRepository;
import org.cardanofoundation.rewards.repository.TxRepository;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Service
public class TxServiceImpl implements TxService {

  TxRepository txRepository;
  BlockRepository blockRepository;

  @Override
  public long getTxIdAdaPotsOfEpoch(int epoch) {
    var block = blockRepository.getFirstBlockByEpochNo(epoch);
    if (block.getTxCount() > 0) {
      return txRepository.getMaxTxIdByBlockId(block.getId());
    }
    return txRepository.getLastTxIdByEpochNo(epoch - 1);
  }

  @Override
  public long getTxIdLedgerSnapshotOfEpoch(int epoch) {
    return txRepository.getLastTxIdByEpochNo(epoch - 1);
  }
}
