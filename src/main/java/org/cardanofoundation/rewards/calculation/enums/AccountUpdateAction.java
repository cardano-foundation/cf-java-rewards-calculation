package org.cardanofoundation.rewards.calculation.enums;

public enum AccountUpdateAction {
    REGISTRATION, DELEGATION, WITHDRAWAL, DEREGISTRATION;

    public static AccountUpdateAction fromString(String action) {
        return switch (action.toUpperCase()) {
            case "REGISTRATION" -> AccountUpdateAction.REGISTRATION;
            case "DELEGATION" -> AccountUpdateAction.DELEGATION;
            case "WITHDRAWAL" -> AccountUpdateAction.WITHDRAWAL;
            case "DEREGISTRATION" -> AccountUpdateAction.DEREGISTRATION;
            default -> throw new IllegalArgumentException("Invalid account update action: " + action);
        };
    }
}
