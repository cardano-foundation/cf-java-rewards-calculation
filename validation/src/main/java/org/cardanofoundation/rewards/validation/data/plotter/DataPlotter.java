package org.cardanofoundation.rewards.validation.data.plotter;

import org.cardanofoundation.rewards.calculation.config.NetworkConfig;

public interface DataPlotter {

    public void plot(int epochStart, int epochEnd, NetworkConfig networkConfig);
}
