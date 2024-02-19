package org.cardanofoundation.rewards.entity.jpa.projection;

import java.math.BigInteger;

public interface MemberReward {
    String getPoolId();
    String getStakeAddress();
    BigInteger getAmount();
}
