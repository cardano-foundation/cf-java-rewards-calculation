package org.cardanofoundation.rewards.data.fetcher;

public interface DataFetcher {
    public void fetch(int epoch, boolean override);
}
