package io.github.foreverstr.autoconfigure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.foreverstr.cache.CacheManager;
import io.github.foreverstr.cache.SecondLevelCache;
import io.github.foreverstr.cache.redis.RedisCacheProperties;
import io.github.foreverstr.cache.impl.NoOpSecondLevelCache;
import io.github.foreverstr.cache.redis.RedisSecondLevelCache;
import io.github.foreverstr.session.factory.Jorm;
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
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

@Configuration
@EnableConfigurationProperties({JormProperties.class, RedisCacheProperties.class})
public class JormAutoConfiguration {
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
        HikariDataSource dataSource = new HikariDataSource(config);
        Jorm.setDataSource(dataSource);
        return dataSource;
    }

    @Bean
    @Primary // 设置为主要的 DataSource，确保其他组件也使用事务感知的数据源
    public DataSource jormDataSource(DataSource dataSource) {
        return new TransactionAwareDataSourceProxy(dataSource);
    }

    @PostConstruct
    public void init() {
        // 获取事务感知的 DataSource 并设置给 JORM
        DataSource ds = applicationContext.getBean("jormDataSource", DataSource.class);
        Jorm.setDataSource(ds);

        // 初始化缓存管理器
        SecondLevelCache cache = applicationContext.getBean(SecondLevelCache.class);
        CacheManager.setSecondLevelCache(cache);
        CacheManager.setCacheEnabled(cacheProperties.isEnabled() && !(cache instanceof NoOpSecondLevelCache));
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

    @PostConstruct
    public void initCacheManager() {
        SecondLevelCache cache = applicationContext.getBean(SecondLevelCache.class);
        CacheManager.setSecondLevelCache(cache);
        CacheManager.setCacheEnabled(cacheProperties.isEnabled() && !(cache instanceof NoOpSecondLevelCache));
    }
}
