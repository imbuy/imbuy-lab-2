package imbuy.lot.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BidClientFallback implements BidClient {

    @Override
    public Long getAuctionWinner(Long lotId) {
        log.warn("Bid service unavailable. Cannot determine winner for lot #{}", lotId);
        return null;
    }
}