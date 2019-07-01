package com.example.demo.service.impl;

import com.example.demo.dao.OrderBookDao;
import com.example.demo.entity.OrderUpdate;
import com.example.demo.service.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderBookServiceImpl implements OrderBookService {

    private final OrderBookDao orderBookDao;

    @Autowired
    public OrderBookServiceImpl(OrderBookDao orderBookDao) {
        this.orderBookDao = orderBookDao;
    }

    @Override
    public List<OrderUpdate> getOrderBook(){
        return orderBookDao.getOrderBook();
    }
}
