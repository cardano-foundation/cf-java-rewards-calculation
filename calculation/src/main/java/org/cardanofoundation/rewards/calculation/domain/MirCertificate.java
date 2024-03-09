package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;
import org.cardanofoundation.rewards.calculation.enums.MirPot;

import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MirCertificate {
    private MirPot pot;
    private BigInteger totalRewards;
}
