package org.cardanofoundation.rewards.validation.entity.jpa.projection;

public interface LatestStakeAccountUpdate {
    public String getStakeAddress();
    public String getLatestUpdateType();
}
