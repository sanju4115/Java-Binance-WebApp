package com.example.demo.config;

import com.binance.api.client.BinanceApiAsyncRestClient;
import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BinanceConfig {

    // use real api key and secret for buy and selling coins
    private final BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance("API-KEY", "SECRET");

    @Bean
    public BinanceApiRestClient getBinanceApiRestClient(){
        return factory.newRestClient();
    }

    @Bean
    public BinanceApiWebSocketClient getBinanceApiWebSocketClient(){
        return factory.newWebSocketClient();
    }

    @Bean
    public BinanceApiAsyncRestClient getBinanceApiAsyncRestClient(){
        return factory.newAsyncRestClient();
    }


}
