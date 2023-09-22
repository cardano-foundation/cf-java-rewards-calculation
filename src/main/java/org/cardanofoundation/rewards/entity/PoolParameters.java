package org.cardanofoundation.rewards.entity;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PoolParameters {
    int epoch;
    Double pledge;
}
