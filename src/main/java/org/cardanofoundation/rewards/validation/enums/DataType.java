package org.cardanofoundation.rewards.validation.enums;

public enum DataType {
    ADA_POTS ("adaPots"),
    EPOCH_INFO ("epochInfo"),
    PROTOCOL_PARAMETERS ("protocolParameters"),
    POOL_DEREGISTRATIONS ("poolDeregistrations"),
    POOL_PARAMETERS ("poolParameters"),
    POOL_HISTORY ("poolHistory"),
    ACCOUNT_UPDATES ("accountUpdates"),
    POOL_OWNER_HISTORY("accountHistory"),
    MIR_CERTIFICATE("mirCertificates"),
    RETIRED_POOLS("retiredPools"),
    MEMBER_REWARDS("memberRewards"),
    REWARDS("rewards"),
    REWARDS_OUTLIER("rewardsOutlier"),
    POOL_BLOCKS("poolBlocks"),
    PAST_ACCOUNT_REGISTRATIONS("pastAccountRegistrations"),
    LATE_DEREGISTRATIONS("lateDeregistrations"),
    ACCOUNT_DEREGISTRATION("accountDeregistrations"),
    PAST_ACCOUNT_REGISTRATIONS_UNTIL_LAST_EPOCH("pastAccountRegistrationsUntilLastEpoch"),
    PAST_ACCOUNT_REGISTRATIONS_UNTIL_NOW("pastAccountRegistrationsUntilNow");

    public final String resourceFolderName;

    DataType(String resourceFolderName) {
        this.resourceFolderName = resourceFolderName;
    }
}
