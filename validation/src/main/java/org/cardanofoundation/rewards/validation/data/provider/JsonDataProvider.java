package org.cardanofoundation.rewards.validation.data.provider;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.domain.EpochValidationInput;
import org.cardanofoundation.rewards.validation.domain.PoolReward;
import org.cardanofoundation.rewards.validation.util.JsonConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class JsonDataProvider implements DataProvider {

    @Value("${json.data-provider.source}")
    private String sourceFolder;

    private int epoch;

    private EpochValidationInput epochValidationInput;

    private void loadEpochValidationInput(int epoch) {
        if (epoch != this.epoch) {
            log.info("Loading epoch validation input for epoch " + epoch + " into memory");

            String filePath = String.format("%s/epoch-validation-input-%d.json.gz", sourceFolder, epoch);
            try {
                this.epochValidationInput = JsonConverter.readJsonFile(filePath, EpochValidationInput.class);
                this.epoch = epoch;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @Override
    public AdaPots getAdaPotsForEpoch(int epoch) {
        loadEpochValidationInput(epoch + 1);
        return AdaPots.builder()
                .treasury(epochValidationInput.getTreasuryOfPreviousEpoch())
                .reserves(epochValidationInput.getReservesOfPreviousEpoch())
                .rewards(BigInteger.ZERO)
                .adaInCirculation(BigInteger.ZERO)
                .epoch(epoch)
                .build();
    }

    @Override
    public Epoch getEpochInfo(int epoch) {
        loadEpochValidationInput(epoch);

        if (epochValidationInput.getBlockCount() == 0) {
            return null;
        }

        return Epoch.builder()
                .blockCount(epochValidationInput.getBlockCount())
                .fees(epochValidationInput.getFees())
                .number(epoch)
                .nonOBFTBlockCount(epochValidationInput.getNonOBFTBlockCount())
                .activeStake(epochValidationInput.getActiveStake())
                .build();
    }

    @Override
    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        loadEpochValidationInput(epoch);
        return ProtocolParameters.builder()
                .decentralisation(epochValidationInput.getDecentralisation())
                .treasuryGrowRate(epochValidationInput.getTreasuryGrowRate())
                .monetaryExpandRate(epochValidationInput.getMonetaryExpandRate())
                .optimalPoolCount(epochValidationInput.getOptimalPoolCount())
                .poolOwnerInfluence(epochValidationInput.getPoolOwnerInfluence())
                .build();
    }

    @Override
    public List<PoolState> getHistoryOfAllPoolsInEpoch(int epoch, List<PoolBlock> blocksMadeByPoolsInEpoch) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getPoolStates().stream().toList();
    }

    @Override
    public PoolState getPoolHistory(String poolId, int epoch) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getPoolStates().stream()
                .filter(poolState -> poolState.getPoolId().equals(poolId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public HashSet<String> getRewardAddressesOfRetiredPoolsInEpoch(int epoch) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getRewardAddressesOfRetiredPoolsInEpoch();
    }

    @Override
    public List<MirCertificate> getMirCertificatesInEpoch(int epoch) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getMirCertificates().stream().toList();
    }

    @Override
    public BigInteger getTransactionDepositsInEpoch(int epoch) {
        return null;
    }

    @Override
    public BigInteger getSumOfFeesInEpoch(int epoch) {
        return null;
    }

    @Override
    public BigInteger getSumOfWithdrawalsInEpoch(int epoch) {
        return null;
    }

    @Override
    public HashSet<Reward> getMemberRewardsInEpoch(int epoch) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getPoolRewards().stream()
                .flatMap(reward -> reward.getDelegatorRewards().stream().map(
                        delegatorReward -> Reward.builder()
                                .poolId(reward.getPoolId())
                                .stakeAddress(delegatorReward.getStakeAddress())
                                .amount(delegatorReward.getReward())
                                .build())
                )
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public List<PoolBlock> getBlocksMadeByPoolsInEpoch(int epoch) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getPoolStates().stream()
                .map(poolState -> PoolBlock.builder()
                        .poolId(poolState.getPoolId())
                        .blockCount(poolState.getBlockCount())
                        .build())
                .toList();
    }

    @Override
    public HashSet<PoolReward> getTotalPoolRewardsInEpoch(int epoch) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getPoolRewards().stream()
                .map(reward -> PoolReward.builder()
                        .poolId(reward.getPoolId())
                        .epoch(epoch)
                        .amount(reward.getTotalPoolReward())
                        .build())
                .collect(Collectors.toCollection(HashSet::new));
    }

    @Override
    public HashSet<String> findSharedPoolRewardAddressWithoutReward(int epoch) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getSharedPoolRewardAddressesWithoutReward();
    }

    @Override
    public HashSet<String> getDeregisteredAccountsInEpoch(int epoch, long stabilityWindow) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getDeregisteredAccounts();
    }

    @Override
    public HashSet<String> getRegisteredAccountsUntilLastEpoch(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getRegisteredAccountsSinceLastEpoch();
    }

    @Override
    public HashSet<String> getRegisteredAccountsUntilNow(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow) {
        loadEpochValidationInput(epoch);
        return epochValidationInput.getRegisteredAccountsUntilNow();
    }

    public EpochValidationInput getEpochValidationInput(int epoch) {
        loadEpochValidationInput(epoch);
        return epochValidationInput;
    }
}
