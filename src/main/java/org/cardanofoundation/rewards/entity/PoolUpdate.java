package org.cardanofoundation.rewards.entity;

import lombok.*;
import org.cardanofoundation.rewards.enums.PoolStatus;

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
    private Double margin;
    private Double fixedCost;
    private Double pledge;
    private String rewardAddress;
    private List<String> owners;
}
