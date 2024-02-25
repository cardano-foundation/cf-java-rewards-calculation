package org.cardanofoundation.rewards.validation.entity.jpa.projection;

import java.util.Date;

public interface StakeAccountUpdate {

    String getAddress();

    String getAction();

    Date getUnixBlockTime();
}
