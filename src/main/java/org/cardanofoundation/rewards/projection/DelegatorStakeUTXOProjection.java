package org.cardanofoundation.rewards.projection;

import java.math.BigInteger;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@AllArgsConstructor
public class DelegatorStakeUTXOProjection {
  Long stakeAddressId;
  BigInteger totalUXTOStake;
  Long poolId;
}
