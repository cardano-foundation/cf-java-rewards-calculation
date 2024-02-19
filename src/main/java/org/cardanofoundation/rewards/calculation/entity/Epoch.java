package org.cardanofoundation.rewards.calculation.entity;

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
    private BigInteger output;
    private BigInteger fees;
    private int blockCount;
    private BigInteger activeStake;
    private List<String> poolsMadeBlocks;
    private int nonOBFTBlockCount;
    private int OBFTBlockCount;
    private long unixTimeFirstBlock;
    private long unixTimeLastBlock;
}
