package org.cardanofoundation.rewards.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdaPots {
    private int epoch;
    private Double treasury;
    private Double reserves;
    private Double rewards;
}
