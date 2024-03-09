package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;

import java.math.BigInteger;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Epoch {
    private int number;
    private BigInteger fees;
    private int blockCount;
    private BigInteger activeStake;
    private int nonOBFTBlockCount;
}
