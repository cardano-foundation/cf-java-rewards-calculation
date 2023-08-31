package org.cardanofoundation.rewards.schedules;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import org.cardanofoundation.rewards.common.EpochEndData;
import org.cardanofoundation.rewards.repository.EpochRepository;
import org.cardanofoundation.rewards.service.AdaPotsService;
import org.cardanofoundation.rewards.service.EpochEndService;

@Slf4j
@Component
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Profile("calculation")
public class EndEpochSchedule {

  AdaPotsService adaPotsService;
  EpochRepository epochRepository;
  EpochEndService epochEndService;

  @Scheduled(fixedDelayString = "${schedule.epoch-scan}")
  public void scanEpochHasChanged() {
    var epoch = epochRepository.getCurrentEpoch();
    var adaPotsEpochCanUpdate = adaPotsService.getLastEpochHasUpdated() + 1;
    if (epoch > adaPotsEpochCanUpdate) {
      for (int e = adaPotsEpochCanUpdate; e < epoch - 2; e++) {
        log.info("Updating epoch {}", e);
        try {
          EpochEndData epochEndData = epochEndService.getEpochEndData(e);
          epochEndService.saveEpochEndData(epochEndData);
        } catch (Exception ex) {
          ex.printStackTrace();
          System.exit(1);
        }
      }
    } else {
      log.info("Epoch {} is not changed yet!", epoch);
    }
  }
}
