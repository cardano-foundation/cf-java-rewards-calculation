package org.cardanofoundation.rewards.entity;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdaPots {
    private int epoch;
    private Double treasury;
    private Double reserves;
    private Double rewards;
    private Double adaInCirculation;
}
