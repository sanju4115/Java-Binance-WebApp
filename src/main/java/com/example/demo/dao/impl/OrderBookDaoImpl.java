package com.example.demo.dao.impl;

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
        QueryResult queryResult = influxDB.query(
                new Query("SELECT * FROM order_book", "mstakx")
        );
        InfluxDBResultMapper resultMapper = new InfluxDBResultMapper(); // thread-safe - can be reused
        return  resultMapper.toPOJO(queryResult, OrderUpdate.class);
    }
}
