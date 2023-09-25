package org.cardanofoundation.rewards.entity;

import lombok.*;
import org.cardanofoundation.rewards.enums.MirPot;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MirCertificate {
    private long blockTime;
    private MirPot pot;
    private int totalStakeKeys;
    private double totalRewards;
}
