package com.example.demo.controller;

import com.example.demo.entity.OrderUpdate;
import com.example.demo.service.OrderBookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/order-book")
public class OrderBookController {

    private final OrderBookService orderBookService;

    @Autowired
    public OrderBookController(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<OrderUpdate>> create(){
        return new ResponseEntity<>(orderBookService.getOrderBook(), HttpStatus.OK);
    }
}
