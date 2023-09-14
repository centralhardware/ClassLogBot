package me.centralhardware.znatoki.telegram.statistic.configuration;

import com.clickhouse.jdbc.ClickHouseDataSource;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.mapper.StatisticMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.TeacherNameMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.TimeMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration
@RequiredArgsConstructor
public class MyBaatisConfiguration {

    @Bean
    public SqlSessionFactory getSqlSesstion(){
        TransactionFactory transactionFactory = new JdbcTransactionFactory();
        Environment environment = new Environment("development", transactionFactory, getDataSource());
        var configuration = new org.apache.ibatis.session.Configuration(environment);
        configuration.addMapper(TimeMapper.class);
        configuration.addMapper(StatisticMapper.class);
        configuration.addMapper(TeacherNameMapper.class);
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    public DataSource getDataSource(){
        try {
            return new ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    public StatisticMapper getStatisticMapper(SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(StatisticMapper.class);
    }

    @Bean
    public TimeMapper getTimeMapper(SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(TimeMapper.class);
    }

    @Bean
    public TeacherNameMapper getTeacherNameMapper(SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(TeacherNameMapper.class);
    }

}
