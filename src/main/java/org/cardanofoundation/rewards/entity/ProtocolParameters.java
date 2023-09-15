package org.cardanofoundation.rewards.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProtocolParameters {
    Double decentralisation;
    Double treasuryGrowRate;
    Double monetaryExpandRate;
    int optimalPoolCount;
    Double poolOwnerInfluence;
}
