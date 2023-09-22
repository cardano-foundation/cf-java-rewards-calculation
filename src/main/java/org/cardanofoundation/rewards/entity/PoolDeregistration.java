package org.cardanofoundation.rewards.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolDeregistration {
    private int epoch;
    private String poolId;
    private String rewardAddress;
}
