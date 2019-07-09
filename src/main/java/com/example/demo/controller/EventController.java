package com.example.demo.controller;

import com.example.demo.handler.OrderBookHandler;
import com.example.demo.model.OrderBookModel;
import com.example.demo.model.PriceAndQuantity;
import com.google.common.io.CharStreams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.BlockingQueue;

@RestController
@RequestMapping("/api/v1/order-book")
public class EventController {
    private final OrderBookHandler orderBookHandler;

    @Autowired
    public EventController(OrderBookHandler orderBookHandler) {
        this.orderBookHandler = orderBookHandler;
    }

    @PostMapping(value = "/add-event/write", consumes = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @ResponseBody
    public void eventAdd(RequestEntity<InputStream> entity) throws IOException, ClassNotFoundException, InterruptedException {
        BlockingQueue<OrderBookModel> queue = orderBookHandler.getQueue();
        InputStream body = entity.getBody();
        String text = null;
        if (body == null)
            return;
        try (final Reader reader = new InputStreamReader(body)) {
            text = CharStreams.toString(reader);
        }
        addToQueue(queue, text);
    }

    private void addToQueue(BlockingQueue<OrderBookModel> queue, String text) throws InterruptedException {
        String[] split = text.split("\n");
        for (String str: split) {
            OrderBookModel orderBookModel = new OrderBookModel();
            int firstSpace = str.indexOf(" ");
            int lastSpace = str.lastIndexOf(" ");
            String time = str.substring(lastSpace+1);
            str = str.substring(firstSpace + 1, lastSpace);
            int askOpenBracket = str.indexOf("{", 0);
            int askClosedBracket = str.indexOf("}", askOpenBracket);
            String askStr = str.substring(askOpenBracket + 1, askClosedBracket);
            String[] orderBookStr = askStr.split(",");
            NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>(Comparator.reverseOrder());
            for (String orderBook: orderBookStr){
                String[] priceAndQty = orderBook.split("=");
                BigDecimal price = BigDecimal.valueOf(Double.valueOf(priceAndQty[0]));
                BigDecimal qty = BigDecimal.valueOf(Double.valueOf(priceAndQty[1]));
                asks.put(price,qty);
            }

            int bestAskBigin = str.indexOf("best_ask", askClosedBracket)+10;
            int bestAskEnd = str.indexOf("\"", bestAskBigin);
            String bestAskStr = str.substring(bestAskBigin, bestAskEnd);
            String[] priceAndQty = bestAskStr.split("=");
            BigDecimal priceBestAsk = BigDecimal.valueOf(Double.valueOf(priceAndQty[0]));
            BigDecimal qtyBestAsk = BigDecimal.valueOf(Double.valueOf(priceAndQty[1]));
            PriceAndQuantity bestAsk = new PriceAndQuantity(priceBestAsk, qtyBestAsk);

            int bestBidBigin = str.indexOf("best_bid", askClosedBracket)+10;
            int bestBidEnd = str.indexOf("\"", bestBidBigin);
            String bestBidStr = str.substring(bestBidBigin, bestBidEnd);
            priceAndQty = bestBidStr.split("=");
            BigDecimal priceBestBid = BigDecimal.valueOf(Double.valueOf(priceAndQty[0]));
            BigDecimal qtyBestBid= BigDecimal.valueOf(Double.valueOf(priceAndQty[1]));
            PriceAndQuantity bestBid = new PriceAndQuantity(priceBestBid, qtyBestBid);

            int bidOpenBracket = str.indexOf("{", bestBidEnd);
            int bidClosedBracket = str.indexOf("}", bidOpenBracket);
            String bidStr = str.substring(bidOpenBracket + 1, bidClosedBracket);
            orderBookStr = bidStr.split(",");
            NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
            for (String orderBook: orderBookStr){
                priceAndQty = orderBook.split("=");
                BigDecimal price = BigDecimal.valueOf(Double.valueOf(priceAndQty[0]));
                BigDecimal qty = BigDecimal.valueOf(Double.valueOf(priceAndQty[1]));
                bids.put(price,qty);
            }

            int eventTimeBegin = str.indexOf("event_time", bidClosedBracket)+10;
            int eventTimeEnd = str.indexOf(",", eventTimeBegin);
            String eventTimeStr = str.substring(eventTimeBegin + 1, eventTimeEnd-1);

            int symbolBegin = str.indexOf("symbol", eventTimeEnd)+8;
            int symbolEnd = str.indexOf("\"", symbolBegin);
            String symbolStr = str.substring(symbolBegin, symbolEnd);
            orderBookModel.setSymbol(symbolStr);
            orderBookModel.setTime(Long.valueOf(time));
            orderBookModel.setEventTime(Long.valueOf(eventTimeStr));
            orderBookModel.setAsks(asks);
            orderBookModel.setBestAsk(bestAsk);
            orderBookModel.setBids(bids);
            orderBookModel.setBestBid(bestBid);
            queue.put(orderBookModel);
        }
    }
}
