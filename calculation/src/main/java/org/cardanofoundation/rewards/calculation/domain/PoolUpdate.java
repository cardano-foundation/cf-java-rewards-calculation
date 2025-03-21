package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolUpdate {
    private String transactionHash;
    private String poolId;
    private Integer activeEpoch;
    private Integer retiringEpoch;
    private BigDecimal margin;
    private BigInteger fixedCost;
    private BigInteger pledge;
    private String rewardAddress;
    private HashSet<String> owners;
}
