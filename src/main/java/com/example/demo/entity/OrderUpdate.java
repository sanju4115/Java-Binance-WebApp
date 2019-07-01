package com.example.demo.entity;

import org.influxdb.annotation.Column;
import org.influxdb.annotation.Measurement;

import java.time.Instant;

@Measurement(name = "order_book")
public class OrderUpdate {

    @Column(name = "time")
    private Instant time;

    @Column(name = "event_time")
    private Long eventTime;

    @Column(name = "symbol")
    private String symbol;

    @Column(name = "asks")
    private String asks;

    @Column(name = "bids")
    private String bids;

    @Column(name = "best_ask")
    private String bestAsk;

    @Column(name = "best_bid")
    private String bestBid;

    public Instant getTime() {
        return time;
    }

    public void setTime(Instant time) {
        this.time = time;
    }

    public Long getEventTime() {
        return eventTime;
    }

    public void setEventTime(Long eventTime) {
        this.eventTime = eventTime;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getAsks() {
        return asks;
    }

    public void setAsks(String asks) {
        this.asks = asks;
    }

    public String getBids() {
        return bids;
    }

    public void setBids(String bids) {
        this.bids = bids;
    }

    public String getBestAsk() {
        return bestAsk;
    }

    public void setBestAsk(String bestAsk) {
        this.bestAsk = bestAsk;
    }

    public String getBestBid() {
        return bestBid;
    }

    public void setBestBid(String bestBid) {
        this.bestBid = bestBid;
    }

    @Override
    public String toString() {
        return "OrderUpdate{" +
                "time=" + time +
                ", eventTime=" + eventTime +
                ", symbol='" + symbol + '\'' +
                ", asks='" + asks + '\'' +
                ", bids='" + bids + '\'' +
                ", bestAsk='" + bestAsk + '\'' +
                ", bestBid='" + bestBid + '\'' +
                '}';
    }
}
