package org.cardanofoundation.rewards.projection;

import java.math.BigInteger;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
public class EpochStakeProjection {
  Long stakeAddressId;
  BigInteger amount;
  Long poolId;
}
