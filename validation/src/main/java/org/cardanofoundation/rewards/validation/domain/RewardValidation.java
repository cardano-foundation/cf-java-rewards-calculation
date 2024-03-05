package org.cardanofoundation.rewards.validation.domain;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardValidation {
    String stakeAddress;
    BigInteger calculatedReward;
    BigInteger expectedReward;
    BigInteger getOffset;
}
