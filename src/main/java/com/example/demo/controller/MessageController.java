package com.example.demo.controller;

import com.example.demo.service.BinanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NavigableMap;


@Controller
public class MessageController {

    @Autowired
    private SimpMessagingTemplate messageTemplate;

    @Autowired
    private BinanceService binanceService;

    @Scheduled(fixedDelay=1000)
    public void priceManualConvert() throws Exception {
        Map<String, Map<String, NavigableMap<BigDecimal, BigDecimal>>> depthCache = binanceService.getDepthCache();
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = mapper.writeValueAsString(depthCache);
        this.messageTemplate.convertAndSend("/stock/price", jsonInString);
    }
}
