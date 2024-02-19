package org.cardanofoundation.rewards.entity.jpa.projection;

import java.math.BigInteger;

public interface TotalPoolRewards {
    String getPoolId();
    BigInteger getAmount();
}
