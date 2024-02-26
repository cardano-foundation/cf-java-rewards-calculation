package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolBlock {
    String poolId;
    Integer blockCount;
}
