package org.cardanofoundation.rewards.config;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.cardanofoundation.rewards.constants.NetworkConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import rest.koios.client.backend.api.epoch.EpochService;
import rest.koios.client.backend.api.network.NetworkService;
import rest.koios.client.backend.factory.BackendFactory;
import rest.koios.client.backend.factory.BackendService;

@Component
@Slf4j
public class KoiosClient {

  @Value("${application.network-magic}")
  private Integer networkMagic;

  private BackendService backendService;

  public NetworkService networkService() {
    return this.backendService.getNetworkService();
  }

  public EpochService epochService() {
    return this.backendService.getEpochService();
  }

  @PostConstruct
  void setBackendService() {
    String networkName = NetworkConstants.getNetworkNameByMagicNumber(networkMagic);
    log.info("Network: {}", networkName);
    this.backendService =
        switch (networkName) {
          case NetworkConstants.MAINNET_NAME -> BackendFactory.getKoiosMainnetService();
          case NetworkConstants.PREPROD_NAME -> BackendFactory.getKoiosPreprodService();
          case NetworkConstants.PREVIEW_NAME -> BackendFactory.getKoiosPreviewService();
          case NetworkConstants.GUILDNET_NAME -> BackendFactory.getKoiosGuildService();
          default -> throw new IllegalStateException("Unexpected value: " + networkName);
        };
  }
}
