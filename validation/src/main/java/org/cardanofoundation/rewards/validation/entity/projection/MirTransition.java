package org.cardanofoundation.rewards.validation.entity.projection;

import java.math.BigInteger;

public interface MirTransition {
    BigInteger getTotalRewards();
    String getPot();
}
