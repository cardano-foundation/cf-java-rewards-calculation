package org.cardanofoundation.rewards.entity.jpa.projection;

public interface LatestStakeAccountUpdate {
    public String getStakeAddress();
    public String getLatestUpdateType();
}
