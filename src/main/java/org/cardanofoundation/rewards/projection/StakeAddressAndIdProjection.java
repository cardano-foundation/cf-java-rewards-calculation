package org.cardanofoundation.rewards.projection;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
public class StakeAddressAndIdProjection {
  String view;
  Long id;
}
