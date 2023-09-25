package org.cardanofoundation.rewards.entity;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Epoch {
    private int number;
    private Double output;
    private Double fees;
    private int blockCount;
    private Double activeStake;
    private List<String> poolsMadeBlocks;
    private int nonOBFTBlockCount;
    private int OBFTBlockCount;
    private long unixTimeFirstBlock;
    private long unixTimeLastBlock;
}
