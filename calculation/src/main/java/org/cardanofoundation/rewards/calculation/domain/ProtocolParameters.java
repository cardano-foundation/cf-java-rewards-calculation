package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolParameters {
    BigDecimal decentralisation;
    BigDecimal treasuryGrowRate;
    BigDecimal monetaryExpandRate;
    Integer optimalPoolCount;
    BigDecimal poolOwnerInfluence;
}
