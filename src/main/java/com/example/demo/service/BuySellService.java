package com.example.demo.service;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.Order;

public interface BuySellService {
    NewOrderResponse action(String symbol, String buyingPrice, OrderSide orderSide, OrderType orderType, String quantity);

    Order orderStatus(NewOrderResponse sellOrderResponse);
}
