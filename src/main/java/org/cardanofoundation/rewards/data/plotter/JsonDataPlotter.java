package org.cardanofoundation.rewards.data.plotter;

import org.cardanofoundation.rewards.calculation.TreasuryCalculation;
import org.cardanofoundation.rewards.data.provider.JsonDataProvider;
import org.cardanofoundation.rewards.entity.TreasuryCalculationResult;
import org.cardanofoundation.rewards.util.JsonConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedHashMap;
import java.util.Map;

public class JsonDataPlotter implements DataPlotter{

    private static final Logger logger = LoggerFactory.getLogger(JsonDataPlotter.class);


    private final JsonDataProvider jsonDataProvider;

    public JsonDataPlotter() {
        this.jsonDataProvider = new JsonDataProvider();
    }

    @Override
    public void plot(int epochStart, int epochEnd) {
        if (epochStart > epochEnd) {
            throw new IllegalArgumentException("epochStart must be less than or equal to epochEnd");
        }

        Map<Integer, TreasuryCalculationResult> epochTreasuryCalculationResultMap = new LinkedHashMap<>();

        for (int epoch = epochStart; epoch < epochEnd; epoch++) {
            TreasuryCalculationResult treasuryCalculationResult = TreasuryCalculation
                    .calculateTreasuryForEpoch(epoch, jsonDataProvider);
            epochTreasuryCalculationResultMap.put(epoch, treasuryCalculationResult);
        }

        try {
            JsonConverter.writeObjectToJsonFile(epochTreasuryCalculationResultMap,
                    "./report/treasury_calculation_result.json");
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.warn("Failed to write treasury calculation result to json file");
        }
    }
}
