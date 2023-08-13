package me.centralhardware.znatoki.telegram.statistic.configuration;

import com.clickhouse.jdbc.ClickHouseDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
public class ClickhouseJdbcConfiguration {

    @Bean
    public DataSource getDataSource() throws SQLException {
        return new ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"));
    }

}
