package org.cardanofoundation.rewards.enums;

public enum DbSyncRewardType {
    LEADER("leader"),
    MEMBER("member"),
    TREASURY("treasury"),
    RESERVES("reserves"),
    REFUND("refund");

    private final String value;

    DbSyncRewardType(String value) {
        this.value = value;
    }

    public static DbSyncRewardType fromValue(String value) {
        for (DbSyncRewardType type : DbSyncRewardType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid DbSyncRewardType value: " + value);
    }

    public String getValue() {
        return value;
    }
}
