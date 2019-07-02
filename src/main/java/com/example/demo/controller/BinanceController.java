package com.example.demo.controller;

import com.example.demo.service.BinanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/binance")
public class BinanceController {

    private final BinanceService binanceService;

    @Autowired
    public BinanceController(BinanceService binanceService) {
        this.binanceService = binanceService;
    }

    @CrossOrigin
    @GetMapping
    @ResponseBody
    public ResponseEntity<List<String>> getSymbols(){
        return new ResponseEntity<>(binanceService.getSymbols(), HttpStatus.OK);
    }
}
