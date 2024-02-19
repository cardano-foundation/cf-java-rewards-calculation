package org.cardanofoundation.rewards.validation.entity.jpa.projection;

import java.math.BigInteger;

public interface LatestPoolUpdate {
    Long getId();
    String getPoolId();
    BigInteger getPledge();
    BigInteger getFixedCost();
    Double getMargin();
    String getRewardAddress();
}
