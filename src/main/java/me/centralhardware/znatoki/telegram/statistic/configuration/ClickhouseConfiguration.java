package me.centralhardware.znatoki.telegram.statistic.configuration;

import com.clickhouse.client.ClickHouseCredentials;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseProtocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClickhouseConfiguration {

    @Bean
    public ClickHouseNode getClickhouseNode(){
        return ClickHouseNode.builder()
                .host(System.getenv("CLICKHOUSE_HOST"))
                .port(ClickHouseProtocol.HTTP)
                .database(System.getenv("CLICKHOUSE_DATABASE"))
                .credentials(ClickHouseCredentials.fromUserAndPassword(System.getenv("CLICKHOUSE_USER"),
                        System.getenv("CLICKHOUSE_PASS")))
                .build();
    }

}
