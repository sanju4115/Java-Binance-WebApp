package com.example.demo.service;

import com.binance.api.client.domain.market.OrderBookEntry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

public interface BinanceService {

    Set<String> getAllPrices();

    void initializeDepthCache(String symbol);

    void startDepthEventStreaming(String symbol);

    void updateOrderBook(NavigableMap<BigDecimal, BigDecimal> lastOrderBookEntries, List<OrderBookEntry> orderBookDeltas);

    NavigableMap<BigDecimal, BigDecimal> getAsks(String symbol);

    NavigableMap<BigDecimal, BigDecimal> getBids(String symbol);

    Map.Entry<BigDecimal, BigDecimal> getBestAsk(String symbol);

    Map.Entry<BigDecimal, BigDecimal> getBestBid(String symbol);

    Map<String, Map<String, NavigableMap<BigDecimal, BigDecimal>>> getDepthCache();

    void saveDepthCache(String symbol, Long eventTime);
}
