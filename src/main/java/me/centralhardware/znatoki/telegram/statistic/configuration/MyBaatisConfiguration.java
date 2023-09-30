package me.centralhardware.znatoki.telegram.statistic.configuration;

import com.clickhouse.jdbc.ClickHouseDataSource;
import lombok.RequiredArgsConstructor;
import me.centralhardware.znatoki.telegram.statistic.Config;
import me.centralhardware.znatoki.telegram.statistic.eav.PropertyDefs;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.*;
import me.centralhardware.znatoki.telegram.statistic.mapper.clickhouse.StatisticMapper;
import me.centralhardware.znatoki.telegram.statistic.mapper.postgres.EmployNameMapper;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.CustomPropertiesTypeHandler;
import me.centralhardware.znatoki.telegram.statistic.typeHandler.UuidTypeHandler;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.UUID;

@SuppressWarnings("resource")
@Configuration
@RequiredArgsConstructor
public class MyBaatisConfiguration {

    @Bean("sqlSessionFactoryClickhouse")
    public SqlSessionFactory getSqlSessionClickhouse(){
        var clickhouse = new Environment("clickhouse", new JdbcTransactionFactory(), getDataSource());
        var configurationClickhouse = new org.apache.ibatis.session.Configuration(clickhouse);

        configurationClickhouse.addMapper(StatisticMapper.class);
        configurationClickhouse.setCacheEnabled(false);

        return new SqlSessionFactoryBuilder().build(configurationClickhouse);
    }

    @Bean("sqlSessionFactoryPostgres")
    public SqlSessionFactory getSqlSessionPostgres(DataSource dataSource){
        var postgres = new Environment("postgres", new ManagedTransactionFactory(), dataSource);
        var configurationPostgres = new org.apache.ibatis.session.Configuration(postgres);

        configurationPostgres.getTypeHandlerRegistry().register(UUID.class, UuidTypeHandler.class);
        configurationPostgres.getTypeHandlerRegistry().register(PropertyDefs.class, CustomPropertiesTypeHandler.class);

        configurationPostgres.addMapper(SessionMapper.class);
        configurationPostgres.addMapper(OrganizationMapper.class);
        configurationPostgres.addMapper(ServicesMapper.class);
        configurationPostgres.addMapper(InvitationMapper.class);
        configurationPostgres.addMapper(EmployNameMapper.class);
        configurationPostgres.addMapper(PaymentMapper.class);
        configurationPostgres.addMapper(ServiceMapper.class);

        configurationPostgres.setCacheEnabled(false);

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
    public ServiceMapper getTimeMapper(@Qualifier("sqlSessionFactoryPostgres") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(ServiceMapper.class);
    }

    @Bean
    public EmployNameMapper getTeacherNameMapper(@Qualifier("sqlSessionFactoryPostgres") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(EmployNameMapper.class);
    }

    @Bean
    public PaymentMapper getPaymentMapper(@Qualifier("sqlSessionFactoryPostgres") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(PaymentMapper.class);
    }

    @Bean
    public SessionMapper getSessionMapper(@Qualifier("sqlSessionFactoryPostgres") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(SessionMapper.class);
    }

    @Bean
    public OrganizationMapper getOrganizationMapper(@Qualifier("sqlSessionFactoryPostgres") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(OrganizationMapper.class);
    }

    @Bean
    public ServicesMapper getServiceMapper(@Qualifier("sqlSessionFactoryPostgres") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(ServicesMapper.class);
    }

    @Bean
    public InvitationMapper getInvitationMapper(@Qualifier("sqlSessionFactoryPostgres") SqlSessionFactory sqlSessionFactory){
        return sqlSessionFactory.openSession().getMapper(InvitationMapper.class);
    }

}
