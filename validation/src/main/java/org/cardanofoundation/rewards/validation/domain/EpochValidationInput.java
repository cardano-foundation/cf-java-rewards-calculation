package org.cardanofoundation.rewards.validation.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.cardanofoundation.rewards.calculation.domain.MirCertificate;
import org.cardanofoundation.rewards.calculation.domain.PoolState;
import org.cardanofoundation.rewards.calculation.domain.RetiredPool;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EpochValidationInput {
    private int epoch;

    private BigInteger treasuryOfPreviousEpoch;
    private BigInteger reservesOfPreviousEpoch;

    private BigDecimal decentralisation;
    private BigDecimal treasuryGrowRate;
    private BigDecimal monetaryExpandRate;
    private Integer optimalPoolCount;
    private BigDecimal poolOwnerInfluence;

    private BigInteger fees;
    private int blockCount;
    private BigInteger activeStake;
    private int nonOBFTBlockCount;

    private Set<RetiredPool> retiredPools;
    private HashSet<String> deregisteredAccounts;
    private HashSet<String> lateDeregisteredAccounts;
    private HashSet<String> registeredAccountsSinceLastEpoch;
    private HashSet<String> registeredAccountsUntilNow;
    private HashSet<String> sharedPoolRewardAddressesWithoutReward;
    private HashSet<String> deregisteredAccountsOnEpochBoundary;

    private HashSet<PoolState> poolStates;
    private HashSet<MirCertificate> mirCertificates;

    private HashSet<EpochValidationPoolReward> poolRewards;
}
