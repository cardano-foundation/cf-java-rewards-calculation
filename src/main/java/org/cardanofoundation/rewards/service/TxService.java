package org.cardanofoundation.rewards.service;

public interface TxService {

  long getTxIdAdaPotsOfEpoch(int epoch);

  long getTxIdLedgerSnapshotOfEpoch(int epoch);
}
