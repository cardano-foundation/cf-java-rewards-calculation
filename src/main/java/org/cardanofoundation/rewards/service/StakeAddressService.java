package org.cardanofoundation.rewards.service;

import java.util.Set;

public interface StakeAddressService {
  Set<Long> getStakeAddressRegisteredTilEpoch(long txId, Set<Long> stakeAddrIds, int epoch);
}
