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

    @Bean
    public BinanceApiRestClient getBinanceApiRestClient(){
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        return factory.newRestClient();
    }

    @Bean
    public BinanceApiWebSocketClient getBinanceApiWebSocketClient(){
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        return factory.newWebSocketClient();
    }

    @Bean
    public BinanceApiAsyncRestClient getBinanceApiAsyncRestClient(){
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        return factory.newAsyncRestClient();
    }


}
