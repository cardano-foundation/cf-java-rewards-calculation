package org.cardanofoundation.rewards.data.plotter;

import org.cardanofoundation.rewards.data.provider.DataProvider;

public interface DataPlotter {

    public void plot(int epochStart, int epochEnd);
}
