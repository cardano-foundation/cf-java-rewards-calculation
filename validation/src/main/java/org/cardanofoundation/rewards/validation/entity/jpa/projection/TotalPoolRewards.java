package org.cardanofoundation.rewards.validation.entity.jpa.projection;

import java.math.BigInteger;

public interface TotalPoolRewards {
    String getPoolId();
    BigInteger getAmount();
}
