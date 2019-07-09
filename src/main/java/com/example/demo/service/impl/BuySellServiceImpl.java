package com.example.demo.service.impl;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;
import com.example.demo.service.BuySellService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BuySellServiceImpl implements BuySellService {

    private static final Logger LOG = LoggerFactory.getLogger(BuySellServiceImpl.class);

    @Override
    public NewOrderResponse action(String symbol, String buyingPrice, OrderSide orderSide, OrderType orderType, String quantity){
        NewOrderResponse buyOrderResponse = null;
        /*buyOrderResponse = binanceApiRestClient.newOrder( // buy order
                new NewOrder(
                        symbol,
                        orderSide,
                        orderType,
                        TimeInForce.GTC,
                        quantity,
                        buyingPrice
                )
        );*/
        LOG.info("BuyOrderResponse: {}", buyOrderResponse);
        return buyOrderResponse;
    }

    @Override
    public Order orderStatus(NewOrderResponse sellOrderResponse){
        Order orderStatus = new Order();
        orderStatus.setStatus(OrderStatus.FILLED);
        /*orderStatus = binanceApiRestClient.getOrderStatus(
                new OrderStatusRequest(sellOrderResponse.getClientOrderId(), sellOrderResponse.getOrderId())
        );*/
        return orderStatus;
    }
}
