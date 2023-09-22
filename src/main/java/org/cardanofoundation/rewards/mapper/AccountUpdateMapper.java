package org.cardanofoundation.rewards.mapper;

import org.cardanofoundation.rewards.entity.AccountUpdate;
import org.cardanofoundation.rewards.enums.AccountUpdateAction;
import rest.koios.client.backend.api.account.model.AccountUpdates;

import java.util.ArrayList;
import java.util.List;

public class AccountUpdateMapper {

    public static List<AccountUpdate> fromKoiosAccountUpdates(List<AccountUpdates> accountUpdates) {
        List<AccountUpdate> accountUpdateList = new ArrayList<>();

        for (AccountUpdates accountUpdate : accountUpdates) {
            for (rest.koios.client.backend.api.account.model.AccountUpdate accountUpdateItem : accountUpdate.getUpdates()) {
                accountUpdateList.add(AccountUpdate.builder()
                        .stakeAddress(accountUpdate.getStakeAddress())
                        .action(AccountUpdateAction.fromString(accountUpdateItem.getActionType()))
                        .transactionHash(accountUpdateItem.getTxHash())
                        .epoch(Math.toIntExact(accountUpdateItem.getEpochNo()))
                        .epochSlot(Long.valueOf(accountUpdateItem.getEpochSlot()))
                        .absoluteSlot(Long.valueOf(accountUpdateItem.getAbsoluteSlot()))
                        .unixBlockTime(Long.valueOf(accountUpdateItem.getBlockTime()))
                        .build());
            }
        }

        return accountUpdateList;
    }
}
