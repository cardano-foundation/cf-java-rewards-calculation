package org.cardanofoundation.rewards.validation.mapper;

import org.cardanofoundation.rewards.calculation.domain.AccountUpdate;
import org.cardanofoundation.rewards.calculation.enums.AccountUpdateAction;
import org.cardanofoundation.rewards.validation.entity.jpa.projection.LatestStakeAccountUpdate;
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

    public static AccountUpdate fromLatestStakeAccountUpdate(LatestStakeAccountUpdate latestStakeAccountUpdate) {
        return AccountUpdate.builder()
                .stakeAddress(latestStakeAccountUpdate.getStakeAddress())
                .action(AccountUpdateAction.fromString(latestStakeAccountUpdate.getLatestUpdateType()))
                .epoch(latestStakeAccountUpdate.getEpoch())
                .epochSlot(latestStakeAccountUpdate.getEpochSlot().longValue())
                .build();
    }
}
