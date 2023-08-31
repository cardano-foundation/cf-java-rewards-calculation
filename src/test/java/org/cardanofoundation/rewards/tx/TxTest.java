package org.cardanofoundation.rewards.tx;

import org.cardanofoundation.rewards.service.TxService;
import org.junit.jupiter.api.Disabled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;

@SpringBootTest
@Disabled
public class TxTest {

  @Autowired
  TxService txService;

  @Test
  void getTxSnapshotEpoch() {
    for (int i = 208; i < 300; i++) {
      long txId = txService.getTxIdLedgerSnapshotOfEpoch(i);
      System.out.println("Epoch " + i + ", txId " + txId);
    }
  }
}
