package com.example.demo.controller;

import com.example.demo.entity.OrderUpdate;
import org.influxdb.InfluxDB;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.impl.InfluxDBResultMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/order-book")
public class OrderBookController {

    private final static Logger LOGGER = LoggerFactory.getLogger(OrderBookController.class);

    @Autowired
    private InfluxDB influxDB;

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<OrderUpdate>> create(){
        QueryResult queryResult = influxDB.query(new Query("SELECT * FROM order_book", "mstakx"));
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper(); // thread-safe - can be reused
        List<OrderUpdate> cpuList = resultMapper.toPOJO(queryResult, OrderUpdate.class);
        return new ResponseEntity<>(cpuList, HttpStatus.OK);
    }
}
