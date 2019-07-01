package com.example.demo.dao;

import com.example.demo.entity.OrderUpdate;

import java.util.List;

public interface OrderBookDao {

    List<OrderUpdate> getOrderBook();
}
