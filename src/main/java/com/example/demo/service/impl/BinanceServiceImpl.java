package com.example.demo.service.impl;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.event.DepthEvent;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;
import com.binance.api.client.domain.market.TickerPrice;
import com.example.demo.service.BinanceService;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class BinanceServiceImpl implements BinanceService {

    private static final Logger LOG = LoggerFactory.getLogger(BinanceServiceImpl.class);

    private static final String BIDS  = "BIDS";
    private static final String ASKS  = "ASKS";

    private Map<String, Long> lastUpdateId = new HashMap<>();

    private Map<String, Map<String, NavigableMap<BigDecimal, BigDecimal>>> depthCache = new HashMap<>();

    @Autowired
    private InfluxDB influxDB;

    @PostConstruct
    public void init() {
        Set<String> allPrices = getAllPrices();
        allPrices.forEach(s -> new Thread(new HandleEvents(s)).start());
    }

    class HandleEvents implements Runnable{

        private String symbol;

        HandleEvents(String symbol) {
            this.symbol = symbol;
        }

        @Override
        public void run() {
            System.out.println("Started thread for "+ symbol);
            initializeDepthCache(symbol);
            startDepthEventStreaming(symbol);
        }
    }

    @Override
    public Set<String> getAllPrices(){
        Set<String> symbols = new HashSet<>();
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        BinanceApiRestClient client = factory.newRestClient();
        List<TickerPrice> allPrices = client.getAllPrices();
        allPrices.stream().filter(tickerPrice -> {
            String symbol = tickerPrice.getSymbol();
            CharSequence charSequence = symbol.subSequence(symbol.length() - 3, symbol.length());
            return charSequence.toString().equals("BTC");
        }).forEach(tickerPrice -> symbols.add(tickerPrice.getSymbol()));
        return symbols;
    }

    /**
     * Initializes the depth cache by using the REST API.
     */
    @Override
    public void initializeDepthCache(String symbol) {
        try {
            BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
            BinanceApiRestClient client = factory.newRestClient();
            OrderBook orderBook = client.getOrderBook(symbol.toUpperCase(), 10);
            this.lastUpdateId.put(symbol, orderBook.getLastUpdateId());
            NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>(Comparator.reverseOrder());
            for (OrderBookEntry ask : orderBook.getAsks()) {
                asks.put(new BigDecimal(ask.getPrice()), new BigDecimal(ask.getQty()));
            }
            HashMap<String, NavigableMap<BigDecimal, BigDecimal>> exchange = new HashMap<>();
            depthCache.put(symbol, exchange);
            exchange.put(ASKS, asks);

            NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
            for (OrderBookEntry bid : orderBook.getBids()) {
                bids.put(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()));
            }
            exchange.put(BIDS, bids);
        }catch (Exception e){
            LOG.error("Error while initializing depth cache for symbol "+ symbol);
        }
    }

    /**
     * Begins streaming of depth events.
     */
    @Override
    public void startDepthEventStreaming(String symbol) {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        BinanceApiWebSocketClient client = factory.newWebSocketClient();

        client.onDepthEvent(symbol.toLowerCase(), new BinanceApiCallback<DepthEvent>() {
            @Override
            public void onResponse(final DepthEvent response) {
                if (response.getUpdateId() > lastUpdateId.get(symbol)) {
                    //System.out.println(response);
                    lastUpdateId.put(symbol,response.getUpdateId());
                    updateOrderBook(getAsks(symbol), response.getAsks());
                    updateOrderBook(getBids(symbol), response.getBids());
                    saveDepthCache(symbol, response.getEventTime());
                }
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOG.error("Web socket failed for "+symbol);
                new Thread(new HandleEvents(symbol)).start();
            }
        });

    }

    /**
     * Updates an order book (bids or asks) with a delta
     * received from the server.
     *
     * Whenever the qty specified is ZERO, it means the price
     * should was removed from the order book.
     */
    @Override
    public void updateOrderBook(NavigableMap<BigDecimal, BigDecimal> lastOrderBookEntries, List<OrderBookEntry> orderBookDeltas) {
        for (OrderBookEntry orderBookDelta : orderBookDeltas) {
            BigDecimal price = new BigDecimal(orderBookDelta.getPrice());
            BigDecimal qty = new BigDecimal(orderBookDelta.getQty());
            if (qty.compareTo(BigDecimal.ZERO) == 0) {
                // qty=0 means remove this level
                lastOrderBookEntries.remove(price);
            } else {
                lastOrderBookEntries.put(price, qty);
            }
        }
    }

    @Override
    public NavigableMap<BigDecimal, BigDecimal> getAsks(String symbol) {
        return depthCache.get(symbol).get(ASKS);
    }

    @Override
    public NavigableMap<BigDecimal, BigDecimal> getBids(String symbol) {
        return depthCache.get(symbol).get(BIDS);
    }

    /**
     * @return the best ask in the order book
     */
    @Override
    public Map.Entry<BigDecimal, BigDecimal> getBestAsk(String symbol) {
        return getAsks(symbol).lastEntry();
    }

    /**
     * @return the best bid in the order book
     */
    @Override
    public Map.Entry<BigDecimal, BigDecimal> getBestBid(String symbol) {
        return getBids(symbol).firstEntry();
    }

    /**
     * @return a depth cache, containing two keys (ASKs and BIDs),
     * and for each, an ordered list of book entries.
     */
    @Override
    public Map<String, Map<String, NavigableMap<BigDecimal, BigDecimal>>> getDepthCache() {
        return depthCache;
    }

    /**
     * save the cached order book / depth of a symbol as well as
     * the best ask and bid price in the book.
     */
    @Override
    public void saveDepthCache(String symbol, Long eventTime) {
        Point point = Point.measurement("order_book")
                .time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
                .addField("event_time", eventTime)
                .addField("symbol", symbol)
                .addField("asks", getAsks(symbol).toString())
                .addField("bids", getBids(symbol).toString())
                .addField("best_ask", getBestAsk(symbol).toString())
                .addField("best_bid", getBestBid(symbol).toString())
                .build();
        influxDB.write(point);
    }
}
