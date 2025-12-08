package imbuy.lot.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "bid-service")
public interface BidClient {

    @GetMapping("/bids/lots/{lotId}/winning")
    Long getAuctionWinner(@PathVariable("lotId") Long lotId);
}