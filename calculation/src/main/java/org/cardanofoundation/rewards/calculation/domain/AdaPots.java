package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaPots {
    private int epoch;
    private BigInteger treasury;
    private BigInteger reserves;
    private BigInteger rewards;
    private BigInteger deposits;
    private BigInteger adaInCirculation;
    private BigInteger fees;
}
