# Cardano Rewards Calculation 🚧️ Under Construction 🚧️

<p align="left">
<img alt="Tests" src="https://github.com/cardano-foundation/cf-java-rewards-calculation/actions/workflows/tests.yaml/badge.svg?branch=main" />
<img alt="Coverage" src="https://github.com/cardano-foundation/cf-java-rewards-calculation/blob/gh-pages/badges/jacoco.svg?raw=true" />
<img alt="Release" src="https://github.com/cardano-foundation/cf-java-rewards-calculation/actions/workflows/release.yaml/badge.svg?branch=main" />
<a href="https://conventionalcommits.org"><img alt="conventionalcommits" src="https://img.shields.io/badge/Conventional%20Commits-1.0.0-%23FE5196?logo=conventionalcommits" /></a>
<a href="https://opensource.org/licenses/MIT"><img alt="License" src="https://img.shields.io/badge/License-MIT-green.svg" /></a>
</p>

This java project is used to calculate the rewards of the Cardano network. It aims to be both an edge case documentation and formula implementation.

```mermaid
flowchart
    A[Total Transaction Fees <br />at Epoch n] --> B[<a href='https://github.com/cardano-foundation/cf-java-rewards-calculation/blob/a794130dc0e320426725a58b8b15f1fbe726b2de/src/main/java/org/cardanofoundation/rewards/calculation/TreasuryCalculation.java#L42'>Total Reward Pot <br />at Epoch n</a/>]
    B --> | <a href='https://cips.cardano.org/cips/cip9/#updatableprotocolparameters'>treasuryGrowthRate</a> | C[<a href='https://github.com/cardano-foundation/cf-java-rewards-calculation/blob/a794130dc0e320426725a58b8b15f1fbe726b2de/src/main/java/org/cardanofoundation/rewards/calculation/TreasuryCalculation.java#L17'>Treasury</a/>]
    B --> | 1 - <a href='https://cips.cardano.org/cips/cip9/#updatableprotocolparameters'>treasuryGrowthRate</a> | D[<a href='https://github.com/cardano-foundation/cf-java-rewards-calculation/blob/a794130dc0e320426725a58b8b15f1fbe726b2de/src/test/java/org/cardanofoundation/rewards/calculation/PoolRewardCalculationTest.java#L63'>Stake Pool Rewards Pot <br />at Epoch n</a/>]
    subgraph ADA_POTS[" "]
    D --> | Unclaimed Rewards | E["ADA Reserves<br /> (monetary expansion) <br /> Started at ~14B ADA"]
    E --> | <a href='https://cips.cardano.org/cips/cip9/#updatableprotocolparameters'>monetaryExpandRate</a> * <a href='https://github.com/cardano-foundation/cf-java-rewards-calculation/blob/main/src/main/java/org/cardanofoundation/rewards/calculation/PoolRewardCalculation.java#L19'>apparent performance of all stake pools</a> | B
    C --> F[Payouts e.g. for <br /><a href='https://projectcatalyst.io/'>Project Catalyst</a>]
    D --> | <a href='https://github.com/cardano-foundation/cf-java-rewards-calculation/blob/a794130dc0e320426725a58b8b15f1fbe726b2de/src/main/java/org/cardanofoundation/rewards/calculation/PoolRewardCalculation.java#L87'>Rewards Equation<br /> for Pool 1</a> | G[Stake Pool 1]
    D --> | <a href='https://github.com/cardano-foundation/cf-java-rewards-calculation/blob/a794130dc0e320426725a58b8b15f1fbe726b2de/src/main/java/org/cardanofoundation/rewards/calculation/PoolRewardCalculation.java#L87'>Rewards Equation<br /> for Pool 2</a> | H[Stake Pool 2]
    D --> I[...]
    D --> | <a href='https://github.com/cardano-foundation/cf-java-rewards-calculation/blob/a794130dc0e320426725a58b8b15f1fbe726b2de/src/main/java/org/cardanofoundation/rewards/calculation/PoolRewardCalculation.java#L87'>Rewards Equation<br /> for Pool n</a> | J[Stake Pool n]
    end

    style A fill:#5C8DFF,stroke:#5C8DFF
    style B fill:#5C8DFF,stroke:#5C8DFF
    style C fill:#1EC198,stroke:#1EC198
    style D fill:#5C8DFF,stroke:#5C8DFF
    style E fill:#1EC198,stroke:#1EC198

    style F fill:#F6C667,stroke:#F6C667
    style G fill:#F6C667,stroke:#F6C667
    style H fill:#F6C667,stroke:#F6C667
    style I fill:#F6C667,stroke:#F6C667
    style J fill:#F6C667,stroke:#F6C667

    style ADA_POTS fill:#f6f9ff,stroke:#f6f9ff
```

## 🚀 Getting Started

#### Prerequisites

Java 17

#### Build & Test

```
git clone https://github.com/cardano-foundation/cf-java-rewards-calculation.git
cd cf-java-rewards-calculation
./mvnw clean test
```

## 🧪 Test Reports

To ensure the stability and reliability of this project, unit tests have been implemented. By clicking on the link below, you can access the detailed test report.

📊 [Coverage Report](https://cardano-foundation.github.io/cf-java-rewards-calculation/coverage-report/)

## 📖 Sources

 - Beavr Cardano Stake Pool: [How is the Rewards Pot (R) Calculated](https://archive.ph/HQfoV/fb8166e31d2bf61d3d6ca769e7785f2a96530f8e.webp)
 - Protocol Parameters: https://beta.explorer.cardano.org/en/protocol-parameters/
