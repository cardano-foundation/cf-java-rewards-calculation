# Changelog

## [0.1.0](https://github.com/cardano-foundation/cf-java-rewards-calculation/compare/v0.0.1...v0.1.0) (2023-09-25)


### Features

* add a json data provider to use local files for the computation ([03f361f](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/03f361f9d701cc2198afbbfad61707247dfc3ee6))
* add data fetcher to provide static files for the json data provider ([2752aef](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/2752aef82c92dd96c21db11cb7f391b734d6f1d7))
* add pool calculation ([f3a3db0](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/f3a3db0a4b3e5f929110a137839518326d64aee1))
* add pool leader and member reward calculation ([a315188](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/a3151888e3133937b6098efdec72b587d88ba4cd))
* add pool reap transition rule ([3bd9765](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/3bd97655eff28106b5a94845430b780d03cf25ad))
* add reporting and export the report to GH pages ([2696bd3](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/2696bd3dfd84f11d1191f719692cd5346e44890f))
* adding a mermaid graph to explain the reward calculation, perform the calculation a test with parameters ([25b62f9](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/25b62f9d6fb4aba6b1f67f051840eef8f9647d98))
* align apparent pool perofmance calculation with mkApparentPerformance from the Haskell cardano-ledger implementation ([c23ff41](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/c23ff41047d22d94cfd84183403fcc77ed1a7881))
* create an abstract data provider and use double instead of BigDecimals ([ed461bc](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/ed461bce97c83f3d50d92e46eba11fda752a5cfc))
* implement using the non obft blocks for the treasury calculation ([9f173c5](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/9f173c5e83cc6724727fcbb4798758e86f688023))
* take care about mir certificates ([33f6113](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/33f6113580effa461d7fc8615a450b0cfac9b09a))


### Bug Fixes

* adjust epochs, subtract pool fees, calculate ada in circulation with reserve from next epoch ([014bbea](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/014bbea41012c6cb26b1b4d3f5c46c538e8cdea3))
* increase version ([d5457d0](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/d5457d0dcb96b5ba4c4126ba233befc1948f277b))
* move actual business logic into pool rewards calculation class ([f0791a9](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/f0791a98dcfd39b15b79b8f9202c750425bc4311))


### Miscellaneous Chores

* release 0.1.0 ([26d7842](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/26d78426eedc5b0baeea8ca0b47d05303892499d))

## 0.0.1 (2023-08-31)


### Features

* extract rewards calculation into an own repository ([7021ce4](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/7021ce445dfba4507f8d8b67e09106512c3cec84))


### Bug Fixes

* jacoco does now produce a test report in the site folder ([7d620b7](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/7d620b7ddf289bae97043adb0a18a0aa011370ac))
