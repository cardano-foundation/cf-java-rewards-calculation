package org.cardanofoundation.rewards.validation.data.plotter;

import org.cardanofoundation.rewards.calculation.domain.EpochCalculationResult;
import org.cardanofoundation.rewards.calculation.domain.PoolState;
import org.cardanofoundation.rewards.validation.EpochValidation;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.validation.domain.EpochValidationInput;
import org.cardanofoundation.rewards.validation.domain.TreasuryValidationResult;
import org.cardanofoundation.rewards.validation.util.CsvConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CsvDataPlotter implements DataPlotter {
    private static final Logger logger = LoggerFactory.getLogger(JsonDataPlotter.class);

    @Autowired
    private JsonDataProvider jsonDataProvider;

    @Override
    public void plot(int epochStart, int epochEnd) {
        if (epochStart > epochEnd) {
            throw new IllegalArgumentException("epochStart must be less than or equal to epochEnd");
        }

        Map<Integer, TreasuryValidationResult> epochTreasuryValidationResultMap = new LinkedHashMap<>();

        List<HashMap<String, String>> data = new ArrayList<>();
        for (int epoch = epochStart; epoch < epochEnd; epoch++) {
            HashMap<String, String> row = new HashMap<>();
            boolean detailedValidation = false;

            EpochValidationInput epochValidationInput = jsonDataProvider.getEpochValidationInput(epoch);
            HashSet<String> poolIds = epochValidationInput.getPoolStates().stream().map(PoolState::getPoolId).collect(Collectors.toCollection(HashSet::new));
            EpochCalculationResult epochCalculationResult = EpochValidation.calculateEpochRewardPots(epoch,
                    jsonDataProvider, detailedValidation);

            row.put("epoch", String.valueOf(epoch));
            row.put("reserves", String.valueOf(epochCalculationResult.getReserves()));
            row.put("treasury", String.valueOf(epochCalculationResult.getTreasury()));
            row.put("epoch_fees", String.valueOf(epochValidationInput.getFees()));
            row.put("unspendable_earned_rewards", String.valueOf(epochCalculationResult.getTreasuryCalculationResult().getUnspendableEarnedRewards()));
            row.put("undistributed_rewards", String.valueOf(epochCalculationResult.getTotalUndistributedRewards()));
            row.put("unclaimed_refunds", String.valueOf(epochCalculationResult.getTreasuryCalculationResult().getUnclaimedRefunds()));
            row.put("treasury_withdrawals", String.valueOf(epochCalculationResult.getTreasuryCalculationResult().getTreasuryWithdrawals()));
            row.put("active_epoch_stake", String.valueOf(epochValidationInput.getActiveStake()));
            row.put("block_count", String.valueOf(epochValidationInput.getBlockCount()));
            row.put("unique_pools_with_blocks", String.valueOf(poolIds.size()));
            data.add(row);
        }

        try {
            CsvConverter.writeObjectToCsvFile(data,
                    "./report/statistics.csv");
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.warn("Failed to write statistics to csv file");
        }
    }
}
