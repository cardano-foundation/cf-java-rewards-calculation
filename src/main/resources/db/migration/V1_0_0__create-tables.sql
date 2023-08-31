CREATE TABLE IF NOT EXISTS epoch_stake_checkpoint
(
    id               bigserial    PRIMARY KEY,
    view             varchar(255) UNIQUE NOT NULL,
    epoch_checkpoint int4         NOT NULL
);

CREATE TABLE IF NOT EXISTS reward_checkpoint
(
    id               bigserial    PRIMARY KEY,
    view             varchar(255) UNIQUE NOT NULL,
    epoch_checkpoint int4         NOT NULL
);

CREATE TABLE IF NOT EXISTS pool_history
(
    id               bigserial    PRIMARY KEY,
    pool_id          int8         UNIQUE NOT NULL,
    epoch_no         int4         UNIQUE NULL,
    active_stake     numeric      NULL,
    active_stake_pct float8       NULL,
    saturation_pct   float8       NULL,
    block_cnt        int4         NULL,
    delegator_cnt    int4         NULL,
    margin           numeric      NULL,
    fixed_cost       numeric      NULL,
    pool_fees        numeric      NULL,
    deleg_rewards    numeric      NULL,
    epoch_ros        numeric      NULL
);

CREATE TABLE IF NOT EXISTS pool_history_checkpoint
(
     id                     bigserial     PRIMARY KEY,
     "view"                 varchar(255)  UNIQUE NOT NULL,
     epoch_checkpoint       int4          NOT NULL,
     is_spendable_reward    bool          NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS pool_info
(
    id               bigserial      PRIMARY KEY,
    pool_id          int8           UNIQUE NOT NULL,
    fetched_at_epoch int4           UNIQUE NOT NULL,
    active_stake     numeric        NULL,
    live_stake       numeric        NULL,
    live_saturation  float8         NULL
);

CREATE TABLE IF NOT EXISTS pool_info_checkpoint
(
    id bigserial PRIMARY KEY,
    "view" varchar(255) UNIQUE NULL,
    epoch_checkpoint int4 NULL
);
