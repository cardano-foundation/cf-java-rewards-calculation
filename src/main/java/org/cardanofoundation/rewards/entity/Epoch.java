package org.cardanofoundation.rewards.entity;

import lombok.*;

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
}
