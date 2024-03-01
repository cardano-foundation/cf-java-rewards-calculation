package org.cardanofoundation.rewards.calculation.domain;

import lombok.*;
import org.cardanofoundation.rewards.calculation.enums.AccountUpdateAction;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountUpdate {
    private String stakeAddress;
    private AccountUpdateAction action;
    private String transactionHash;
    private Integer epoch;
    private Long epochSlot;
    private Long absoluteSlot;
    private Long unixBlockTime;
}
