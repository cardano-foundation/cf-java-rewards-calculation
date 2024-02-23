package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolDeregistration {
    private int retiringEpoch;
    private String poolId;
    private String rewardAddress;
    private String announcedTransactionHash;
    private long announcedTransactionId;
}
