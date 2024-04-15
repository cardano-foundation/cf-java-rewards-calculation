package org.cardanofoundation.rewards.validation.domain;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpochValidationDelegatorReward {
    String stakeAddress;
    BigInteger reward;
}
