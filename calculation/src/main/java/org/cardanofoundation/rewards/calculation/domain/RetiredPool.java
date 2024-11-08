package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;

import java.math.BigInteger;
import java.util.Objects;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetiredPool {
    private String poolId;
    private String rewardAddress;
    private BigInteger depositAmount;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RetiredPool that = (RetiredPool) o;
        return Objects.equals(poolId, that.poolId) && Objects.equals(rewardAddress, that.rewardAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(poolId, rewardAddress);
    }
}
