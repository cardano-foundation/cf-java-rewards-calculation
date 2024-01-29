package org.cardanofoundation.rewards.data.fetcher;

import org.springframework.stereotype.Service;

@Service
public interface DataFetcher {
    void fetch(int epoch, boolean override);
}
