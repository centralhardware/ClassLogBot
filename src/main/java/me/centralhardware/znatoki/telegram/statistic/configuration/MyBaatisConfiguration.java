package me.centralhardware.znatoki.telegram.statistic.configuration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.PupilMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.StatisticMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@RequiredArgsConstructor
public class MyBaatisConfiguration {

    private final ConfigurableBeanFactory beanFactory;
    private final DataSource dataSource;

    @Bean
    public SqlSessionFactory getSqlSesstion(){
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, dataSource);
        var configuration = new org.apache.ibatis.session.Configuration(environment);
        configuration.addMapper(TimeMapper.class);
        configuration.addMapper(StatisticMapper.class);
        configuration.addMapper(PupilMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    @Bean
    public PupilMapper getPupilMapper(SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(PupilMapper.class);
    }

    @Bean
    public StatisticMapper getStatisticMapper(SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(StatisticMapper.class);
    }

    @Bean
    public TimeMapper getTimeMapper(SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(TimeMapper.class);
    }

}
