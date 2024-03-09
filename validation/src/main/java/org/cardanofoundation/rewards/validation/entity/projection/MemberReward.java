package org.cardanofoundation.rewards.validation.entity.projection;

import java.math.BigInteger;

public interface MemberReward {
    String getPoolId();
    String getStakeAddress();
    BigInteger getAmount();
}
