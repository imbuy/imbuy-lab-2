package imbuy.lot.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "bid-service", fallback = BidClientFallback.class)
public interface BidClient {

    @GetMapping("/bids/lots/{lotId}/winner")
    Long getAuctionWinner(@PathVariable("lotId") Long lotId);
}