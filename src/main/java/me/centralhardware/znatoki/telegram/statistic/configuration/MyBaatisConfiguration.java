package me.centralhardware.znatoki.telegram.statistic.configuration;

import com.clickhouse.jdbc.ClickHouseDataSource;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.StatisticMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TeacherNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.TimeMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.SessionMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

@SuppressWarnings("resource")
@Configuration
@RequiredArgsConstructor
public class MyBaatisConfiguration {

    @Bean("sqlSessionFactoryClickhouse")
    public SqlSessionFactory getSqlSessionClickhouse(){
        var clickhouse = new Environment("clickhouse", new JdbcTransactionFactory(), getDataSource());
        var configurationClickhouse = new org.apache.ibatis.session.Configuration(clickhouse);
        configurationClickhouse.addMapper(TimeMapper.class);
        configurationClickhouse.addMapper(StatisticMapper.class);
        configurationClickhouse.addMapper(TeacherNameMapper.class);

        return new SqlSessionFactoryBuilder().build(configurationClickhouse);
    }

    @Bean("sqlSessionFactoryPostgres")
    public SqlSessionFactory getSqlSessionPostgres(DataSource dataSource){
        var postgres = new Environment("postgres", new JdbcTransactionFactory(), dataSource);
        var configurationPostgres = new org.apache.ibatis.session.Configuration(postgres);
        configurationPostgres.addMapper(SessionMapper.class);

        return new SqlSessionFactoryBuilder().build(configurationPostgres);
    }

    public DataSource getDataSource(){
        try {
            return new ClickHouseDataSource(Config.getClickhouseUrl());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public StatisticMapper getStatisticMapper(@Qualifier("sqlSessionFactoryClickhouse") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(StatisticMapper.class);
    }

    @Bean
    public TimeMapper getTimeMapper(@Qualifier("sqlSessionFactoryClickhouse") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(TimeMapper.class);
    }

    @Bean
    public TeacherNameMapper getTeacherNameMapper(@Qualifier("sqlSessionFactoryClickhouse") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(TeacherNameMapper.class);
    }

    @Bean
    public SessionMapper getSessionMapper(@Qualifier("sqlSessionFactoryPostgres") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(SessionMapper.class);
    }

}
