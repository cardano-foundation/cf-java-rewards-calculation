package org.cardanofoundation.rewards.validation.entity.jpa.projection;

import org.cardanofoundation.rewards.calculation.enums.MirPot;

import java.math.BigInteger;

public interface MirTransition {
    BigInteger getTotalRewards();
    String getPot();
}
