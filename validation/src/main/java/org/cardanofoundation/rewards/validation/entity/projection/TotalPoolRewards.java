package org.cardanofoundation.rewards.validation.entity.projection;

import java.math.BigInteger;

public interface TotalPoolRewards {
    String getPoolId();
    BigInteger getAmount();
}
