package org.cardanofoundation.rewards.calculation.entity;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class TreasuryCalculationResult {
    int epoch;
    BigInteger treasury;
    BigInteger totalRewardPot;
    BigInteger treasuryWithdrawals;
}
