package org.cardanofoundation.rewards.entity;

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
