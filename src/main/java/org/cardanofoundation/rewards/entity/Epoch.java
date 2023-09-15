package org.cardanofoundation.rewards.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Epoch {
    private int number;
    private Double output;
    private Double fees;
    private int blockCount;
    private Double activeStake;
}
