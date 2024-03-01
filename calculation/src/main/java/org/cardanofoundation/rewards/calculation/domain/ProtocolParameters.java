package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import static org.cardanofoundation.rewards.calculation.constants.RewardConstants.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocolParameters {
    BigDecimal decentralisation;
    BigDecimal treasuryGrowRate;
    BigDecimal monetaryExpandRate;
    Integer optimalPoolCount;
    BigDecimal poolOwnerInfluence;

    public BigDecimal getDecentralisation() {
        return Objects.requireNonNullElse(decentralisation, MAINNET_SHELLEY_START_DECENTRALISATION);
    }

    public BigDecimal getTreasuryGrowRate() {
        return Objects.requireNonNullElse(treasuryGrowRate, MAINNET_SHELLEY_START_TREASURY_GROW_RATE);
    }

    public BigDecimal getMonetaryExpandRate() {
        return Objects.requireNonNullElse(monetaryExpandRate, MAINNET_SHELLEY_START_MONETARY_EXPAND_RATE);
    }

    public Integer getOptimalPoolCount() {
        return Objects.requireNonNullElse(optimalPoolCount, MAINNET_SHELLEY_START_OPTIMAL_POOL_COUNT);
    }

    public BigDecimal getPoolOwnerInfluence() {
        return Objects.requireNonNullElse(poolOwnerInfluence, MAINNET_SHELLEY_START_POOL_OWNER_INFLUENCE);
    }

}
