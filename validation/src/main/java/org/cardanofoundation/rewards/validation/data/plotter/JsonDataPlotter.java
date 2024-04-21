package org.cardanofoundation.rewards.validation.data.plotter;

import org.cardanofoundation.rewards.calculation.domain.AdaPots;
import org.cardanofoundation.rewards.calculation.domain.EpochCalculationResult;
import org.cardanofoundation.rewards.validation.EpochValidation;
import org.cardanofoundation.rewards.validation.TreasuryValidation;
import org.cardanofoundation.rewards.validation.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.calculation.domain.TreasuryCalculationResult;
import org.cardanofoundation.rewards.validation.domain.TreasuryValidationResult;
import org.cardanofoundation.rewards.validation.util.JsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class JsonDataPlotter implements DataPlotter {

    private static final Logger logger = LoggerFactory.getLogger(JsonDataPlotter.class);

    @Autowired
    private JsonDataProvider jsonDataProvider;

    @Override
    public void plot(int epochStart, int epochEnd) {
        if (epochStart > epochEnd) {
            throw new IllegalArgumentException("epochStart must be less than or equal to epochEnd");
        }

        Map<Integer, TreasuryValidationResult> epochTreasuryValidationResultMap = new LinkedHashMap<>();

        for (int epoch = epochStart; epoch < epochEnd; epoch++) {
            boolean detailedValidation = false;
            EpochCalculationResult epochCalculationResult = EpochValidation.calculateEpochRewardPots(epoch,
                    jsonDataProvider, detailedValidation);
            TreasuryValidationResult treasuryValidationResult = TreasuryValidationResult
                    .fromTreasuryCalculationResult(epochCalculationResult.getTreasuryCalculationResult());
            AdaPots adaPotsForCurrentEpoch = jsonDataProvider.getAdaPotsForEpoch(epoch);

            if (adaPotsForCurrentEpoch != null) {
                treasuryValidationResult.setActualTreasury(adaPotsForCurrentEpoch.getTreasury());
                epochTreasuryValidationResultMap.put(epoch, treasuryValidationResult);
            } else {
                logger.warn("Failed to get AdaPots for epoch " + epoch + ". Skipping epoch.");
            }
        }

        try {
            JsonConverter.writeObjectToJsonFile(epochTreasuryValidationResultMap,
                    "./report/treasury_calculation_result.json");
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.warn("Failed to write treasury calculation result to json file");
        }
    }
}
