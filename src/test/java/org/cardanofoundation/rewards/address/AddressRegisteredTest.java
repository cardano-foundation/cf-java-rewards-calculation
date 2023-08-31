package org.cardanofoundation.rewards.address;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import org.cardanofoundation.rewards.service.TxService;
import org.cardanofoundation.rewards.service.impl.StakeAddressServiceImpl;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;

@SpringBootTest
@Disabled
public class AddressRegisteredTest {

  @Autowired
  StakeAddressServiceImpl stakeAddressService;

  @Autowired
  TxService txService;

  // stake key :stake1u8fpv84fvlscglyyaamj58ngcu4v9effttm49zw05d2aslq6jnzpj
  @Test
  void Test_checkAddressIsRegisteredInEpoch213() {
    long txId = 2596083;
    var stakeAddressId = 37999L;
    var result =
        stakeAddressService.getStakeAddressRegisteredTilEpoch(
            txId, new HashSet<>(List.of(37999L)), 213);
    assertTrue(result.contains(stakeAddressId));
  }

  @Test
  void Test_checkAddressIsRegisteredInEpoch215() {
    long stakeAddressId = 14572L;
    int epoch = 216;
    var txId = txService.getTxIdLedgerSnapshotOfEpoch(epoch);
    var result =
        stakeAddressService.getStakeAddressRegisteredTilEpoch(
            txId, new HashSet<>(List.of(stakeAddressId)), epoch);
    assertFalse(result.contains(stakeAddressId));
  }
}
