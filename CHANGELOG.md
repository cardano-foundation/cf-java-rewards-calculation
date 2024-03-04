# Changelog

## [0.6.2](https://github.com/cardano-foundation/cf-java-rewards-calculation/compare/v0.6.1...v0.6.2) (2024-03-04)


### Bug Fixes

* get correct registration state in case of late re-registrations ([d5dc65e](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/d5dc65e23e27e86eb364746b5c0bbd981f5aebed))

## [0.6.1](https://github.com/cardano-foundation/cf-java-rewards-calculation/compare/v0.6.0...v0.6.1) (2024-03-02)


### Bug Fixes

* add a workaround to fix an issue with the block count in dbsync epoch table ([5603d5f](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/5603d5f66076ecca6d9437250b1d3096fc97e6e8))

## [0.6.0](https://github.com/cardano-foundation/cf-java-rewards-calculation/compare/v0.5.0...v0.6.0) (2024-03-02)


### Features

* add epoch calculation and refactor validation class ([9fcadab](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/9fcadabd53a55d5f9bf5a3c2226a510c7fab1d7f))
* create an overall epoch calculation for all pots ([3cdee2e](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/3cdee2e7492e4f7e9fab6df5f002c553c1066cbe))
* handle pool owner rewards in pool reward calculation ([81e3ee7](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/81e3ee78bddd1c201ff99ebab7136c76c40157a7))
* handle Shelly ADA flow correctly ([0524849](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/0524849acafec020d3fd01d3eb518b6df3a75b81))
* implement missing methods in JSON data provider to run a whole epoch calculation ([312bfb5](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/312bfb5b7e2ef377070847bcd0fbaf288426ca5a))
* improve validation speed as well ([6f35fc5](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/6f35fc50628b163b7a7347130cdd690e47c76bd2))
* reduce epoch calculation time from 30 min to 20 sec. ([c5f90e7](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/c5f90e75c0bc416d8ae60fb200430aa491d34344))
* use BigInteger to prevent rounding issues. Utxo pot is now 100% correct ([dc0c7af](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/dc0c7af8ba1af7b95c344f9f7feb2ba7eeb00e38))
* use HashSet to improve the performance ([60ea7d2](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/60ea7d297e5a4830133e4e6fbe44f8d04a29934b))
* whole epoch calculation correct for epoch 213 ([1a48a89](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/1a48a89661ca6169f9f5ca96f40d9b0d6529c806))


### Bug Fixes

* add outlier and null checks so that epoch 214 is now also 100% correct ([e10ada0](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/e10ada0d4a0cdca35453c7d35f0bf06eb2d496a0))
* ajust plot and report feature according to the latest changes ([1e6b38e](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/1e6b38e937ae06ac3a3d98cf5e8aa16814ef890d))
* change epoch for pool pledge ([b980b47](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/b980b475ff3aced74955c8374c8469c212728f57))
* correct epoch for getting the latest pool update ([493b66e](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/493b66e123b62e1efabbc922192eafc983ba3f18))
* implemented reward union like described in 17.4 of the shelly ledger spec. The reserves are now almost correct with dbsync until epoch 350 ([088779a](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/088779a98291d9c2f5b2363bce1848225888c8d4))
* introduce BigDecimals to prevent rounding issues and add a floor rounding to the pool reward pot calculation ([18211c5](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/18211c5aac9a3a8e8ee3dca02ffcf8dfa260c41b))
* repair tests ([235f9d1](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/235f9d1182d1b0ee703d24793cb31cc90efb6d02))
* resolve bug in method to fetch the pool owner from the latest update id ([097d308](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/097d3081d11d06fc0763e456e0dd8da877c4f63c))

## [0.5.0](https://github.com/cardano-foundation/cf-java-rewards-calculation/compare/v0.4.0...v0.5.0) (2024-01-29)


### Features

* add more information to the treasury report ([2da66f9](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/2da66f90503b3e0bd0ec069ef85a389adc58481b))

## [0.4.0](https://github.com/cardano-foundation/cf-java-rewards-calculation/compare/v0.3.0...v0.4.0) (2024-01-27)


### Features

* download aggregated data from the s3 bucket during the report generation. closes [#22](https://github.com/cardano-foundation/cf-java-rewards-calculation/issues/22) ([267b035](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/267b035f4eed4444d42f666740e2c62ef8ae3046))
* fetch test data from the s3 bucket while running the test ([4be8758](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/4be8758282d4d4799bdfe40a69f68be9d4c150fb))


### Bug Fixes

* repair publish pipeline ([6409772](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/640977269c402a4ad314465c0edade2f9f1adbb0))

## [0.3.0](https://github.com/cardano-foundation/cf-java-rewards-calculation/compare/v0.2.0...v0.3.0) (2024-01-21)


### Features

* add fee calculation to calculate the fee pot for a certain epoch ([9cfadf7](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/9cfadf7f17d4f02fce037760c67beef26129b53f))
* add utxo pot calculation to calculation module ([e5ac721](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/e5ac7213d9171fcadd71dda51d918f654eb6aabf))


### Bug Fixes

* add pom to pipeline ([3784c3a](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/3784c3a1eaed5200dbc9b5942b469012cb3e10ee))

## [0.2.0](https://github.com/cardano-foundation/cf-java-rewards-calculation/compare/v0.1.0...v0.2.0) (2024-01-21)


### Features

* add dbsync data fetcher ([51a2972](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/51a297242eecc086074869e801b23a1ab70c8a5f))
* add fees pot calculation as a sum from the tx fees of a previous epoch ([97323f8](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/97323f801c645115f75b743af3810d20a08bd571))
* add method to fetch pool blocks per epoch ([13688d2](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/13688d28993093154e5c8b4fc6e61a764e714bef))
* add utxo pot calculation ([8edc86f](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/8edc86fe32eb696d2938eacda1dedbd6fcf7cfb5))
* **dbsync_dataprovider:** improve pool calculation speed ([2c463cf](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/2c463cfeedb7f71504474a9ad5916cf27d2a41b9))
* finish get pool history in dbsync data provider ([6462e43](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/6462e4352b9e3e5bd1934ab9a5a79483c82cd46b))
* finish the implementation of the dbsync data provider ([e22cc0c](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/e22cc0c1668c25472913566289991cb8eaf85fba))
* join dbsync tables to get the epoch stake information ([07b33e9](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/07b33e9dd4af4bf89229162a290d811f0d69cf16))
* start implementation of a dbsync data provider ([8965ba3](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/8965ba31ce1399b585a448ef8cdbdb6b7bc5ad05))
* start with calculation for all ada pots in an epoch ([225e91e](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/225e91efc84dd8b372562f1359e0da74f149717b))
* start with deposits calculation ([c9c06a8](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/c9c06a8981605904e6b106559b128ca9575dc1a8))
* use the env file to configure the json data source path ([58062df](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/58062df6d24e6349268dfdffc668d7d3ea7a3a65))


### Bug Fixes

* add a &lt;= to the select query to find updates after the retirement because this could also happen epochs before the actual retiring epoch ([55213eb](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/55213eb6c1a4a31caeff5963521ae099b844d84f))
* **dbsync_dataprovider:** add epoch stake as active stake to the epoch info ([87cbf80](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/87cbf80534a4850f8738a7d7200d5df918aba46a))
* deposits calculation is now 100% alinged with the ada_pots table. Fix bug about double deregistrations and epoch switches ([fcbb4b3](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/fcbb4b34ed7645156c2ab0c94a76760dc2263e76))
* implement deposits calculation including the correct epoch boundary handling ([5b0405a](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/5b0405ac82c6361dc52cda73aacecf951d00a90d))
* resolve an issue with not finding pool updates for a certain epoch ([10f3355](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/10f3355c30fc7c45a9d40d98e18593d07a002268))
* use manifest-pr command to fix release pipeline ([9259820](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/925982008719357bdc6d54285a64fda70a762b46))
* use non obft blocks if d&gt;0 and d&lt;0.8 and do not decide epoch-based ([3165b92](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/3165b92dbbecf2653fa6daa914bdd5f627d5bd6e))


### Miscellaneous Chores

* release 0.2.0 ([2195551](https://github.com/cardano-foundation/cf-java-rewards-calculation/commit/219555178d73c2fc747b7a154d3ce3ea42bfb746))

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
