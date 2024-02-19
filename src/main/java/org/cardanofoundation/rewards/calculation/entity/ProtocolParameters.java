package org.cardanofoundation.rewards.calculation.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolParameters {
    Double decentralisation;
    Double treasuryGrowRate;
    Double monetaryExpandRate;
    int optimalPoolCount;
    Double poolOwnerInfluence;
}
