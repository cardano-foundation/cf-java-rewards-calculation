package org.cardanofoundation.rewards.entity.jpa.projection;

import java.util.Date;

public interface StakeAccountUpdate {

    String getAddress();

    String getAction();

    Date getUnixBlockTime();
}
