package org.cardanofoundation.rewards.entity;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class TreasuryCalculationResult {
    int epoch;
    Double calculatedTreasury;
    Double actualTreasury;
    Double totalRewardPot;
}
