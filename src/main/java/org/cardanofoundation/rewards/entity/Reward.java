package org.cardanofoundation.rewards.entity;

import lombok.*;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reward {
    private BigInteger amount;
    private String stakeAddress;
}
