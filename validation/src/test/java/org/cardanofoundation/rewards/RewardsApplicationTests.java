package org.cardanofoundation.rewards;

import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = RewardsApplication.class)
@TestPropertySource(locations = { "classpath:application.yaml" })
class RewardsApplicationTests {
  @Test
  void contextLoads() {}
}
