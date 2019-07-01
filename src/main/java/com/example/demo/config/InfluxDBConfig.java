package com.example.demo.config;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Pong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class InfluxDBConfig {

    // CREATE USER sanjay WITH PASSWORD 'timeseries4days' WITH ALL PRIVILEGES
    // CREATE DATABASE mstakx
    // CREATE RETENTION POLICY "two_hours_mstakx" ON "mstakx" DURATION 2h REPLICATION 1 DEFAULT

    private static final Logger LOG = LoggerFactory.getLogger(InfluxDBConfig.class);

    @Value("${influx.db.url}")
    private String influxDBUrl;

    @Value("${influx.db.user}")
    private String influxDBUser;

    @Value("${influx.db.password}")
    private String influxDBPassword;

    @Bean
    public InfluxDB influxDB() {
        InfluxDB influxDB = InfluxDBFactory.connect(influxDBUrl, influxDBUser, influxDBPassword);
        Pong response = influxDB.ping();
        if (response.getVersion().equalsIgnoreCase("unknown")) {
            LOG.error("Error pinging influx db server.");
        }else {
            LOG.info("************ influx db connected *************");
        }
        influxDB.enableBatch(100, 20000, TimeUnit.MILLISECONDS);
        influxDB.setRetentionPolicy("two_hours_mstakx");
        influxDB.setDatabase("mstakx");
        return influxDB;
    }
}
