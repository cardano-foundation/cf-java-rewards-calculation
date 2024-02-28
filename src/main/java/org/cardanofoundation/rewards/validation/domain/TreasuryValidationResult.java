package org.cardanofoundation.rewards.validation.domain;

import lombok.*;
import org.cardanofoundation.rewards.calculation.domain.TreasuryCalculationResult;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreasuryValidationResult {
    int epoch;
    BigInteger calculatedTreasury;
    BigInteger totalRewardPot;
    BigInteger treasuryWithdrawals;
    BigInteger actualTreasury;
    BigInteger unspendableEarnedRewards;

    public static TreasuryValidationResult fromTreasuryCalculationResult(TreasuryCalculationResult treasuryCalculationResult) {
        if (treasuryCalculationResult == null) {
            return null;
        }

        return TreasuryValidationResult.builder()
                .epoch(treasuryCalculationResult.getEpoch())
                .calculatedTreasury(treasuryCalculationResult.getTreasury())
                .totalRewardPot(treasuryCalculationResult.getTotalRewardPot())
                .treasuryWithdrawals(treasuryCalculationResult.getTreasuryWithdrawals())
                .unspendableEarnedRewards(treasuryCalculationResult.getUnspendableEarnedRewards())
                .build();
    }
}
