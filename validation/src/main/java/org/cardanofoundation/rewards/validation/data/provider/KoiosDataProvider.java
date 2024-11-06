package org.cardanofoundation.rewards.validation.data.provider;

import com.bloxbean.cardano.client.common.ADAConversionUtil;
import lombok.RequiredArgsConstructor;
import org.cardanofoundation.rewards.calculation.config.NetworkConfig;
import org.cardanofoundation.rewards.calculation.domain.*;
import org.cardanofoundation.rewards.validation.domain.PoolReward;
import org.cardanofoundation.rewards.validation.mapper.*;
import org.springframework.stereotype.Service;
import rest.koios.client.backend.api.base.exception.ApiException;
import rest.koios.client.backend.api.block.model.Block;
import rest.koios.client.backend.api.epoch.model.EpochInfo;
import rest.koios.client.backend.api.epoch.model.EpochParams;
import rest.koios.client.backend.api.network.model.Totals;
import rest.koios.client.backend.api.pool.model.PoolDelegatorHistory;
import rest.koios.client.backend.api.pool.model.PoolUpdate;
import rest.koios.client.backend.factory.BackendFactory;
import rest.koios.client.backend.factory.BackendService;
import rest.koios.client.backend.factory.options.*;
import rest.koios.client.backend.factory.options.filters.Filter;
import rest.koios.client.backend.factory.options.filters.FilterType;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KoiosDataProvider implements DataProvider {
    private final BackendService koiosBackendService;

    public KoiosDataProvider() {
        this.koiosBackendService = BackendFactory.getKoiosMainnetService();
    }

    public AdaPots getAdaPotsForEpoch(int epoch) {
        Totals totals = null;

        try {
            totals = koiosBackendService.getNetworkService()
                    .getHistoricalTokenomicStatsByEpoch(epoch)
                    .getValue();
        } catch (ApiException e) {
            e.printStackTrace();
        }

        return AdaPotsMapper.fromKoiosTotals(totals);
    }

    public Epoch getEpochInfo(int epoch, NetworkConfig networkConfig) {
        Epoch epochEntity;

        try {
            EpochInfo epochInfo = koiosBackendService.getEpochService()
                    .getEpochInformationByEpoch(epoch).getValue();

            epochEntity = EpochMapper.fromKoiosEpochInfo(epochInfo);
            List<Block> blocks = new ArrayList<>();
            for (int offset = 0; offset < epochEntity.getBlockCount(); offset += 1000) {
                blocks.addAll(koiosBackendService.getBlockService().getBlockList(Options.builder()
                        .option(Filter.of("epoch_no", FilterType.EQ, String.valueOf(epoch)))
                        .option(Offset.of(offset))
                        .build()).getValue());
            }

            if (epoch < 211) {
                epochEntity.setNonOBFTBlockCount(0);
            } else if (epoch > 256) {
                epochEntity.setNonOBFTBlockCount(epochEntity.getBlockCount());
            } else {
                epochEntity.setNonOBFTBlockCount((int) blocks.stream().filter(block -> block.getPool() != null).count());
            }
        } catch (ApiException e) {
            e.printStackTrace();
            return null;
        }

        return epochEntity;
    }

    public ProtocolParameters getProtocolParametersForEpoch(int epoch) {
        EpochParams epochParams = null;

        try {
            epochParams = koiosBackendService.getEpochService()
                    .getEpochParametersByEpoch(epoch).getValue();
        } catch (ApiException e) {
            e.printStackTrace();
        }

        if (epochParams == null) return new ProtocolParameters();

        return ProtocolParametersMapper.fromKoiosEpochParams(epochParams);
    }

    @Override
    public List<PoolState> getHistoryOfAllPoolsInEpoch(int epoch, List<PoolBlock> blocksMadeByPoolsInEpoch) {
        return null;
    }

    public PoolState getPoolHistory(String poolId, int epoch) {
        rest.koios.client.backend.api.pool.model.PoolHistory poolHistory = null;

        try {
            poolHistory = koiosBackendService.getPoolService()
                    .getPoolHistoryByEpoch(poolId, epoch, Options.EMPTY).getValue();
        } catch (ApiException e) {
            e.printStackTrace();
        }

        PoolState history = PoolHistoryMapper.fromKoiosPoolHistory(poolHistory);

        if (history == null) return null;

        HashSet<Delegator> poolMemberInEpoch = getPoolMemberInEpoch(poolId, epoch);
        history.setDelegators(poolMemberInEpoch);
        return history;
    }

    @Override
    public Set<RetiredPool> getRetiredPoolsInEpoch(int epoch) {
        // TODO: It seems as this is not a sufficient method to get the retired pools
        try {
            List<PoolUpdate> poolUpdateList = koiosBackendService.getPoolService().getPoolUpdates(Options.builder()
                .option(Filter.of("active_epoch_no", FilterType.EQ, String.valueOf(epoch)))
                .option(Filter.of("retiring_epoch", FilterType.GT, String.valueOf(epoch - 1)))
                .option(Filter.of("retiring_epoch", FilterType.LTE, String.valueOf(epoch)))
                .build()).getValue();

            if (poolUpdateList == null) return new HashSet<>();

            return poolUpdateList.stream()
                            .map(poolUpdate -> new RetiredPool(poolUpdate.getPoolIdBech32(), poolUpdate.getRewardAddr(), ADAConversionUtil.adaToLovelace(500)))
                                    .collect(Collectors.toSet());
        } catch (ApiException e) {
            e.printStackTrace();
        }

        return Collections.emptySet();
    }

    @Override
    public List<MirCertificate> getMirCertificatesInEpoch(int epoch) {
        return null;
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
        return null;
    }

    @Override
    public List<PoolBlock> getBlocksMadeByPoolsInEpoch(int epoch) {
        return null;
    }

    @Override
    public HashSet<PoolReward> getTotalPoolRewardsInEpoch(int epoch) {
        return null;
    }

    @Override
    public HashSet<String> findSharedPoolRewardAddressWithoutReward(int epoch) {
        return null;
    }

    @Override
    public HashSet<String> getDeregisteredAccountsInEpoch(int epoch, long stabilityWindow) {
        return null;
    }

    @Override
    public HashSet<String> getRegisteredAccountsUntilLastEpoch(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow) {
        return null;
    }

    @Override
    public HashSet<String> getRegisteredAccountsUntilNow(Integer epoch, HashSet<String> stakeAddresses, Long stabilityWindow) {
        return null;
    }

    private HashSet<Delegator> getPoolMemberInEpoch(String poolId, int epoch) {
        HashSet<Delegator> delegators = new HashSet<>();
        try {
            List<PoolDelegatorHistory> poolDelegatorsHistory = koiosBackendService
                    .getPoolService().getPoolDelegatorsHistory(poolId, epoch, Options.EMPTY).getValue();
            for (PoolDelegatorHistory poolDelegator : poolDelegatorsHistory) {
                delegators.add(Delegator.builder()
                        .activeStake(new BigInteger(poolDelegator.getAmount()))
                        .stakeAddress(poolDelegator.getStakeAddress())
                        .build());
            }
        } catch (ApiException e) {
            e.printStackTrace();
        }
        return delegators;
    }
}
