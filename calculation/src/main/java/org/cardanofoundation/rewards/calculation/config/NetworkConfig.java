package org.cardanofoundation.rewards.calculation.config;

import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NetworkConfig {
    private int networkMagic;
    private BigInteger totalLovelace;
    private BigInteger poolDepositInLovelace;
    private long expectedSlotsPerEpoch;
    private BigInteger mainnetShelleyInitialReserves;
    private BigInteger mainnetShelleyInitialTreasury;
    private BigInteger mainnetShelleyInitialUtxo;
    private int genesisConfigSecurityParameter;
    private int mainnetShelleyStartEpoch;
    private int mainnetAllegraHardforkEpoch;
    private int mainnetVasilHardforkEpoch;
    private BigInteger mainnetBootstrapAddressAmount;
    private double activeSlotCoefficient;
    private long randomnessStabilisationWindow;
    private BigDecimal mainnetShelleyStartDecentralisation;
    private BigDecimal mainnetShelleyStartTreasuryGrowRate;
    private BigDecimal mainnetShelleyStartMonetaryExpandRate;
    private int mainnetShelleyStartOptimalPoolCount;
    private BigDecimal mainnetShelleyStartPoolOwnerInfluence;

    public static final int MAINNET_NETWORK_MAGIC = 764824073;
    public static final int PREPROD_NETWORK_MAGIC = 1;
    public static final int PREVIEW_NETWORK_MAGIC = 2;
    public static final int SANCHONET_NETWORK_MAGIC = 4;

    public static NetworkConfig getMainnetConfig() {
        return NetworkConfig.builder()
                .networkMagic(764824073)
                .totalLovelace(new BigInteger("45000000000000000"))
                .poolDepositInLovelace(BigInteger.valueOf(500000000))
                .expectedSlotsPerEpoch(432000)
                .mainnetShelleyInitialReserves(new BigInteger("13888022852926644"))
                .mainnetShelleyInitialTreasury(new BigInteger("0"))
                .mainnetShelleyInitialUtxo(new BigInteger("31111977147073356"))
                .genesisConfigSecurityParameter(2160)
                .mainnetShelleyStartEpoch(208)
                .mainnetAllegraHardforkEpoch(236)
                .mainnetVasilHardforkEpoch(365)
                .mainnetBootstrapAddressAmount(new BigInteger("318200635000000"))
                .activeSlotCoefficient(0.05)
                .randomnessStabilisationWindow(172800)
                .mainnetShelleyStartDecentralisation(BigDecimal.valueOf(1.0))
                .mainnetShelleyStartTreasuryGrowRate(BigDecimal.valueOf(0.2))
                .mainnetShelleyStartMonetaryExpandRate(BigDecimal.valueOf(0.003))
                .mainnetShelleyStartOptimalPoolCount(150)
                .mainnetShelleyStartPoolOwnerInfluence(BigDecimal.valueOf(0.03))
                .build();
    }

    public static NetworkConfig getPreprodConfig() {
        return NetworkConfig.builder()
                .networkMagic(1)
                .totalLovelace(new BigInteger("45000000000000000"))
                .poolDepositInLovelace(BigInteger.valueOf(500000000))
                .expectedSlotsPerEpoch(432000)
                .mainnetShelleyInitialReserves(new BigInteger("14991000000000000"))
                .mainnetShelleyInitialTreasury(new BigInteger("9000000000000"))
                .mainnetShelleyInitialUtxo(new BigInteger("30009000000000000"))
                .genesisConfigSecurityParameter(2160)
                .mainnetShelleyStartEpoch(4)
                .mainnetAllegraHardforkEpoch(5)
                .mainnetVasilHardforkEpoch(12)
                .mainnetBootstrapAddressAmount(new BigInteger("0"))
                .activeSlotCoefficient(0.05)
                .randomnessStabilisationWindow(172800) // (4 * GENESIS_CONFIG_SECURITY_PARAMETER) / ACTIVE_SLOT_COEFFICIENT
                .mainnetShelleyStartDecentralisation(BigDecimal.valueOf(1.0))
                .mainnetShelleyStartTreasuryGrowRate(BigDecimal.valueOf(0.2))
                .mainnetShelleyStartMonetaryExpandRate(BigDecimal.valueOf(0.003))
                .mainnetShelleyStartOptimalPoolCount(150)
                .mainnetShelleyStartPoolOwnerInfluence(BigDecimal.valueOf(0.03))
                .build();
    }

    public static NetworkConfig getPreviewConfig() {
        return NetworkConfig.builder()
                .networkMagic(2)
                .totalLovelace(new BigInteger("45000000000000000"))
                .poolDepositInLovelace(BigInteger.valueOf(500000000))
                .expectedSlotsPerEpoch(86400)
                .mainnetShelleyInitialReserves(new BigInteger("14991000000000000"))
                .mainnetShelleyInitialTreasury(new BigInteger("9000000000000"))
                .mainnetShelleyInitialUtxo(new BigInteger("30009000000000000"))
                .genesisConfigSecurityParameter(432)
                .mainnetShelleyStartEpoch(1)
                .mainnetAllegraHardforkEpoch(1)
                .mainnetVasilHardforkEpoch(3)
                .mainnetBootstrapAddressAmount(new BigInteger("0"))
                .activeSlotCoefficient(0.05)
                .randomnessStabilisationWindow(34560) // (4 * GENESIS_CONFIG_SECURITY_PARAMETER) / ACTIVE_SLOT_COEFFICIENT
                .mainnetShelleyStartDecentralisation(BigDecimal.valueOf(1.0))
                .mainnetShelleyStartTreasuryGrowRate(BigDecimal.valueOf(0.2))
                .mainnetShelleyStartMonetaryExpandRate(BigDecimal.valueOf(0.003))
                .mainnetShelleyStartOptimalPoolCount(150)
                .mainnetShelleyStartPoolOwnerInfluence(BigDecimal.valueOf(0.03))
                .build();
    }

    public static NetworkConfig getSanchonetConfig() {
        return NetworkConfig.builder()
                .networkMagic(2)
                .totalLovelace(new BigInteger("45000000000000000"))
                .poolDepositInLovelace(BigInteger.valueOf(500000000))
                .expectedSlotsPerEpoch(86400)
                .mainnetShelleyInitialReserves(new BigInteger("14991000000000000"))
                .mainnetShelleyInitialTreasury(new BigInteger("9000000000000"))
                .mainnetShelleyInitialUtxo(new BigInteger("30009000000000000"))
                .genesisConfigSecurityParameter(432)
                .mainnetShelleyStartEpoch(1)
                .mainnetAllegraHardforkEpoch(1)
                .mainnetVasilHardforkEpoch(3)
                .mainnetBootstrapAddressAmount(new BigInteger("0"))
                .activeSlotCoefficient(0.05)
                .randomnessStabilisationWindow(34560) // (4 * GENESIS_CONFIG_SECURITY_PARAMETER) / ACTIVE_SLOT_COEFFICIENT
                .mainnetShelleyStartDecentralisation(BigDecimal.valueOf(1.0))
                .mainnetShelleyStartTreasuryGrowRate(BigDecimal.valueOf(0.2))
                .mainnetShelleyStartMonetaryExpandRate(BigDecimal.valueOf(0.003))
                .mainnetShelleyStartOptimalPoolCount(150)
                .mainnetShelleyStartPoolOwnerInfluence(BigDecimal.valueOf(0.03))
                .build();
    }

    public static NetworkConfig getNetworkConfigByNetworkMagic(int networkMagic) {
        return switch (networkMagic) {
            case MAINNET_NETWORK_MAGIC -> getMainnetConfig();
            case PREPROD_NETWORK_MAGIC -> getPreprodConfig();
            case PREVIEW_NETWORK_MAGIC -> getPreviewConfig();
            case SANCHONET_NETWORK_MAGIC -> getSanchonetConfig();
            default -> throw new IllegalArgumentException("Invalid network magic: " + networkMagic);
        };
    }

}
