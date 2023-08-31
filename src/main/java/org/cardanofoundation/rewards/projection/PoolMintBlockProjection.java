package org.cardanofoundation.rewards.projection;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PoolMintBlockProjection {
  Long poolId;
  Long totalBlock;
}
