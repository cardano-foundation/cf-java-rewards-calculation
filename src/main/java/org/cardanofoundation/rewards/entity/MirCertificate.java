package org.cardanofoundation.rewards.entity;

import lombok.*;
import org.cardanofoundation.rewards.enums.MirPot;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MirCertificate {
    private long blockTime;
    private MirPot pot;
    private int totalStakeKeys;
    private BigInteger totalRewards;
}
