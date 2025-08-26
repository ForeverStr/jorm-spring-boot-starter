package io.github.foreverstr.autoconfigure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.foreverstr.cache.CacheManager;
import io.github.foreverstr.cache.SecondLevelCache;
import io.github.foreverstr.cache.redis.RedisCacheProperties;
import io.github.foreverstr.cache.impl.NoOpSecondLevelCache;
import io.github.foreverstr.cache.redis.RedisSecondLevelCache;
import io.github.foreverstr.session.factory.Jorm;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties({JormProperties.class, RedisCacheProperties.class})
public class JormAutoConfiguration implements SmartInitializingSingleton {
    private final JormProperties properties;
    private final RedisCacheProperties cacheProperties;
    private final ApplicationContext applicationContext;

    @Autowired
    public JormAutoConfiguration(JormProperties properties,
                                 RedisCacheProperties cacheProperties,
                                 ApplicationContext applicationContext) {
        this.properties = properties;
        this.cacheProperties = cacheProperties;
        this.applicationContext = applicationContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(properties.getJdbcUrl());
        config.setUsername(properties.getUsername());
        config.setPassword(properties.getPassword());
        config.setDriverClassName(properties.getDriverClassName());
        config.setMaximumPoolSize(properties.getMaximumPoolSize());
        config.setMinimumIdle(properties.getMinimumIdle());
        config.setConnectionTimeout(properties.getConnectionTimeout());
        config.setIdleTimeout(properties.getIdleTimeout());
        config.setMaxLifetime(properties.getMaxLifetime());
        config.setAutoCommit(false);
        return new HikariDataSource(config);
    }

    @Bean
    @ConditionalOnMissingBean
    public PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Bean
    @Primary
    public DataSource jormDataSource(DataSource dataSource) {
        return new TransactionAwareDataSourceProxy(dataSource);
    }

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @ConditionalOnProperty(prefix = "jorm.cache.redis", name = "enabled", havingValue = "true")
    public SecondLevelCache redisSecondLevelCache(RedisTemplate<String, Object> redisTemplate) {
        return new RedisSecondLevelCache(redisTemplate, cacheProperties);
    }

    @Bean
    @ConditionalOnMissingBean(SecondLevelCache.class)
    public SecondLevelCache noOpSecondLevelCache() {
        return new NoOpSecondLevelCache();
    }

    @Override
    public void afterSingletonsInstantiated() {
        // 初始化JORM数据源
        DataSource ds = applicationContext.getBean("jormDataSource", DataSource.class);
        Jorm.setDataSource(ds);

        // 初始化缓存管理器
        SecondLevelCache cache = applicationContext.getBean(SecondLevelCache.class);
        CacheManager.setSecondLevelCache(cache);
        CacheManager.setCacheEnabled(cacheProperties.isEnabled() && !(cache instanceof NoOpSecondLevelCache));
    }
}