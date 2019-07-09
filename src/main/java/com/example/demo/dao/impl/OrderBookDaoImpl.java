package com.example.demo.dao.impl;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.general.ExchangeInfo;
import com.binance.api.client.domain.market.*;
import com.example.demo.dao.OrderBookDao;
import com.example.demo.entity.OrderUpdate;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class OrderBookDaoImpl implements OrderBookDao {

    private final InfluxDB influxDB;

    @Autowired
    public OrderBookDaoImpl(InfluxDB influxDB) {
        this.influxDB = influxDB;
    }

    @Override
    public List<OrderUpdate> getOrderBook(){
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        BinanceApiRestClient client = factory.newRestClient();
        ExchangeInfo exchangeInfo = client.getExchangeInfo();
        TickerStatistics neoeth = client.get24HrPriceStatistics("NEOETH");
        List<BookTicker> bookTickers = client.getBookTickers();
        List<Candlestick> neoeth1 = client.getCandlestickBars("NEOETH", CandlestickInterval.DAILY);
        List<TickerStatistics> list = client.getAll24HrPriceStatistics();
        List<TickerPrice> tickers = client.getAllPrices();
        QueryResult queryResult = influxDB.query(
                new Query("SELECT * FROM order_book", "mstakx")
        );
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper(); // thread-safe - can be reused
        return  resultMapper.toPOJO(queryResult, OrderUpdate.class);
    }
}
