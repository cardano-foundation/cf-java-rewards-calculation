package org.cardanofoundation.rewards.entity.persistence;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregationsInEpoch {
    Double sumOfFees;
    Double sumOfWithdrawals;
    Double sumOfDeposits;
}
