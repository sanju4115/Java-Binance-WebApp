package com.example.demo.service;

import com.example.demo.entity.OrderUpdate;

import java.util.List;

public interface OrderBookService {
    List<OrderUpdate> getOrderBook();
}
