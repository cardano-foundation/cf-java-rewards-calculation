package org.cardanofoundation.rewards.enums;

public enum PoolStatus {
    REGISTERED, RETIRING, RETIRED;

    public static PoolStatus fromString(String status) {
        return switch (status.toUpperCase()) {
            case "REGISTERED" -> PoolStatus.REGISTERED;
            case "RETIRING" -> PoolStatus.RETIRING;
            case "RETIRED" -> PoolStatus.RETIRED;
            default -> throw new IllegalArgumentException("Invalid pool status: " + status);
        };
    }
}
