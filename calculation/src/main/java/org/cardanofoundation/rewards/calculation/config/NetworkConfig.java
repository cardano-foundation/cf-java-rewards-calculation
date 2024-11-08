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
    private BigInteger shelleyInitialReserves;
    private BigInteger shelleyInitialTreasury;
    private BigInteger shelleyInitialUtxo;
    private int genesisConfigSecurityParameter;
    private int shelleyStartEpoch;
    private int allegraHardforkEpoch;
    private int vasilHardforkEpoch;
    private BigInteger bootstrapAddressAmount;
    private double activeSlotCoefficient;
    private long randomnessStabilisationWindow;
    private BigDecimal shelleyStartDecentralisation;
    private BigDecimal shelleyStartTreasuryGrowRate;
    private BigDecimal shelleyStartMonetaryExpandRate;
    private int shelleyStartOptimalPoolCount;
    private BigDecimal shelleyStartPoolOwnerInfluence;

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
                .shelleyInitialReserves(new BigInteger("13888022852926644"))
                .shelleyInitialTreasury(new BigInteger("0"))
                .shelleyInitialUtxo(new BigInteger("31111977147073356"))
                .genesisConfigSecurityParameter(2160)
                .shelleyStartEpoch(208)
                .allegraHardforkEpoch(236)
                .vasilHardforkEpoch(365)
                .bootstrapAddressAmount(new BigInteger("318200635000000"))
                .activeSlotCoefficient(0.05)
                .randomnessStabilisationWindow(172800)
                .shelleyStartDecentralisation(BigDecimal.valueOf(1.0))
                .shelleyStartTreasuryGrowRate(BigDecimal.valueOf(0.2))
                .shelleyStartMonetaryExpandRate(BigDecimal.valueOf(0.003))
                .shelleyStartOptimalPoolCount(150)
                .shelleyStartPoolOwnerInfluence(BigDecimal.valueOf(0.03))
                .build();
    }

    public static NetworkConfig getPreprodConfig() {
        return NetworkConfig.builder()
                .networkMagic(1)
                .totalLovelace(new BigInteger("45000000000000000"))
                .poolDepositInLovelace(BigInteger.valueOf(500000000))
                .expectedSlotsPerEpoch(432000)
                .shelleyInitialReserves(new BigInteger("15000000000000000"))
                .shelleyInitialTreasury(new BigInteger("0"))
                .shelleyInitialUtxo(new BigInteger("30000000000000000"))
                .genesisConfigSecurityParameter(2160)
                .shelleyStartEpoch(4)
                .allegraHardforkEpoch(5)
                .vasilHardforkEpoch(12)
                .bootstrapAddressAmount(new BigInteger("0"))
                .activeSlotCoefficient(0.05)
                .randomnessStabilisationWindow(172800) // (4 * GENESIS_CONFIG_SECURITY_PARAMETER) / ACTIVE_SLOT_COEFFICIENT
                .shelleyStartDecentralisation(BigDecimal.valueOf(1.0))
                .shelleyStartTreasuryGrowRate(BigDecimal.valueOf(0.2))
                .shelleyStartMonetaryExpandRate(BigDecimal.valueOf(0.003))
                .shelleyStartOptimalPoolCount(150)
                .shelleyStartPoolOwnerInfluence(BigDecimal.valueOf(0.03))
                .build();
    }

    public static NetworkConfig getPreviewConfig() {
        return NetworkConfig.builder()
                .networkMagic(2)
                .totalLovelace(new BigInteger("45000000000000000"))
                .poolDepositInLovelace(BigInteger.valueOf(500000000))
                .expectedSlotsPerEpoch(86400)
                .shelleyInitialReserves(new BigInteger("14991000000000000"))
                .shelleyInitialTreasury(new BigInteger("9000000000000"))
                .shelleyInitialUtxo(new BigInteger("30009000000000000"))
                .genesisConfigSecurityParameter(432)
                .shelleyStartEpoch(1)
                .allegraHardforkEpoch(1)
                .vasilHardforkEpoch(3)
                .bootstrapAddressAmount(new BigInteger("0"))
                .activeSlotCoefficient(0.05)
                .randomnessStabilisationWindow(34560) // (4 * GENESIS_CONFIG_SECURITY_PARAMETER) / ACTIVE_SLOT_COEFFICIENT
                .shelleyStartDecentralisation(BigDecimal.valueOf(1.0))
                .shelleyStartTreasuryGrowRate(BigDecimal.valueOf(0.2))
                .shelleyStartMonetaryExpandRate(BigDecimal.valueOf(0.003))
                .shelleyStartOptimalPoolCount(150)
                .shelleyStartPoolOwnerInfluence(BigDecimal.valueOf(0.03))
                .build();
    }

    public static NetworkConfig getSanchonetConfig() {
        return NetworkConfig.builder()
                .networkMagic(2)
                .totalLovelace(new BigInteger("45000000000000000"))
                .poolDepositInLovelace(BigInteger.valueOf(500000000))
                .expectedSlotsPerEpoch(86400)
                .shelleyInitialReserves(new BigInteger("14991000000000000"))
                .shelleyInitialTreasury(new BigInteger("9000000000000"))
                .shelleyInitialUtxo(new BigInteger("30009000000000000"))
                .genesisConfigSecurityParameter(432)
                .shelleyStartEpoch(1)
                .allegraHardforkEpoch(1)
                .vasilHardforkEpoch(3)
                .bootstrapAddressAmount(new BigInteger("0"))
                .activeSlotCoefficient(0.05)
                .randomnessStabilisationWindow(34560) // (4 * GENESIS_CONFIG_SECURITY_PARAMETER) / ACTIVE_SLOT_COEFFICIENT
                .shelleyStartDecentralisation(BigDecimal.valueOf(1.0))
                .shelleyStartTreasuryGrowRate(BigDecimal.valueOf(0.2))
                .shelleyStartMonetaryExpandRate(BigDecimal.valueOf(0.003))
                .shelleyStartOptimalPoolCount(150)
                .shelleyStartPoolOwnerInfluence(BigDecimal.valueOf(0.03))
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
